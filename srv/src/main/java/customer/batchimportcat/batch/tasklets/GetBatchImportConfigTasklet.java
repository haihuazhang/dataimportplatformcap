package customer.batchimportcat.batch.tasklets;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;

import com.sap.cds.Result;
import com.sap.cds.Row;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.CqnValidationException;
import com.sap.cds.services.cds.CqnService;

import cds.gen.dataimportservice.BatchImportConfig_;
import cds.gen.dataimportservice.BatchImportField_;
import cds.gen.dataimportservice.BatchImportFile_;
import cds.gen.dataimportservice.BatchImportStructure_;
import cds.gen.dataimportservice.DataImportService_;
import customer.batchimportcat.batch.dynamic.DynamicConfigurationBuilder;
import customer.batchimportcat.batch.dynamic.DynamicImportConfiguration;
import customer.batchimportcat.batch.dynamic.types.DynamicFieldDefinition;
import customer.batchimportcat.batch.dynamic.types.DynamicStructureDefinition;
import customer.batchimportcat.batch.exceptions.BatchExceptionsUtil;
import customer.batchimportcat.batch.exceptions.BatchFileNotFound;

public class GetBatchImportConfigTasklet implements Tasklet {
    @Qualifier(DataImportService_.CDS_NAME)
    private CqnService dataimportService;

    private DynamicConfigurationBuilder dynamicConfigurationBuilder;

    @org.springframework.beans.factory.annotation.Autowired
    public void setDataimportService(@Qualifier(DataImportService_.CDS_NAME) CqnService dataimportService) {
        this.dataimportService = dataimportService;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void setDynamicConfigurationBuilder(DynamicConfigurationBuilder dynamicConfigurationBuilder) {
        this.dynamicConfigurationBuilder = dynamicConfigurationBuilder;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        String fileUUID = String.valueOf(chunkContext.getStepContext().getJobParameters().get("fileUUID"));

        try {
            Result fileResult = dataimportService
                    .run(Select.from(BatchImportFile_.class).where(file -> file.ID().eq(fileUUID)));
            if (fileResult.rowCount() <= 0) {
                throw BatchExceptionsUtil.getBatchFileNotFound(fileUUID);
            }

            Row fileRow = fileResult.first().orElseThrow(() -> BatchExceptionsUtil.getBatchFileNotFound(fileUUID));
            byte[] fileContent = readFileContent(fileRow);
            String configUUID = String.valueOf(fileRow.get("ConfigUUID"));

            Result configResult = dataimportService.run(
                    Select.from(BatchImportConfig_.class).where(config -> config.ID().eq(configUUID)));
            if (configResult.rowCount() <= 0) {
                throw BatchExceptionsUtil.geBatchConfigNotFound(configUUID);
            }

            Map<String, Serializable> configData = toSerializableMap(configResult.first().orElseThrow());
            List<Map<String, Serializable>> structureRows = toSerializableRows(dataimportService.run(
                    Select.from(BatchImportStructure_.class)
                            .where(structure -> structure.ConfigUUID().eq(configUUID))));
            List<Map<String, Serializable>> fieldRows = toSerializableRows(dataimportService.run(
                    Select.from(BatchImportField_.class)
                            .where(field -> field.ConfigUUID().eq(configUUID))));

            DynamicImportConfiguration dynamicConfig = dynamicConfigurationBuilder.build(configData,
                    structureRows, fieldRows);
            validate(dynamicConfig);

            contribution.getStepExecution().getJobExecution().getExecutionContext().put("fileContent", fileContent);
            contribution.getStepExecution().getJobExecution().getExecutionContext().put("configData", configData);
            contribution.getStepExecution().getJobExecution().getExecutionContext().put("dynamicConfig", dynamicConfig);
        } catch (CqnValidationException exception) {
            throw new BatchFileNotFound(exception.getMessage());
        }

        return RepeatStatus.FINISHED;
    }

    private byte[] readFileContent(Row fileRow) throws Exception {
        InputStream inputStream = (InputStream) fileRow.get("Attachment");
        if (inputStream == null) {
            throw BatchExceptionsUtil.getBatchFileNotFound(String.valueOf(fileRow.get("ID")));
        }
        return inputStream.readAllBytes();
    }

    private List<Map<String, Serializable>> toSerializableRows(Result result) {
        List<Map<String, Serializable>> rows = new ArrayList<>();
        for (Row row : result.listOf(Row.class)) {
            rows.add(toSerializableMap(row));
        }
        return rows;
    }

    private Map<String, Serializable> toSerializableMap(Row row) {
        Map<String, Serializable> serializableData = new HashMap<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getValue() instanceof Serializable serializable) {
                serializableData.put(entry.getKey(), serializable);
            }
        }
        return serializableData;
    }

    private void validate(DynamicImportConfiguration configuration) {
        if (configuration.structures().isEmpty()) {
            throw BatchExceptionsUtil.getBatchConfigNotFoundBecauseNoStruct(configuration.id());
        }

        if (configuration.rootStructures().isEmpty()) {
            throw new IllegalStateException("At least one root structure is required.");
        }

        Set<String> sheetNames = new HashSet<>();
        for (DynamicStructureDefinition structure : configuration.structures()) {
            if (structure.sheetName() == null || structure.sheetName().isBlank()) {
                throw new IllegalStateException("Sheet name is required for every structure.");
            }
            sheetNames.add(structure.sheetName());
            if (structure.sortedFields().isEmpty()) {
                throw new IllegalStateException("At least one field is required for sheet " + structure.sheetName() + ".");
            }
            if (structure.sortedFields().stream().noneMatch(DynamicFieldDefinition::keyField)) {
                throw new IllegalStateException("At least one key field is required for sheet " + structure.sheetName() + ".");
            }
        }

        Map<String, String> parentBySheetName = new HashMap<>();
        for (DynamicStructureDefinition structure : configuration.structures()) {
            if (!structure.rootNode()) {
                if (structure.sheetNameUp() == null || structure.sheetNameUp().isBlank()) {
                    throw new IllegalStateException("Parent sheet name is required for child sheet " + structure.sheetName() + ".");
                }
                if (!sheetNames.contains(structure.sheetNameUp())) {
                    throw new IllegalStateException("Parent sheet " + structure.sheetNameUp() + " does not exist.");
                }
                if (structure.sortedFields().stream().noneMatch(DynamicFieldDefinition::foreignField)) {
                    throw new IllegalStateException("At least one foreign field mapping is required for child sheet "
                            + structure.sheetName() + ".");
                }
                parentBySheetName.put(structure.sheetName(), structure.sheetNameUp());
            }
        }
        ensureNoCycles(parentBySheetName);
    }

    private void ensureNoCycles(Map<String, String> parentBySheetName) {
        for (String sheetName : parentBySheetName.keySet()) {
            Set<String> currentPath = new HashSet<>();
            ArrayDeque<String> stack = new ArrayDeque<>();
            stack.push(sheetName);
            while (!stack.isEmpty()) {
                String current = stack.pop();
                if (!currentPath.add(current)) {
                    throw new IllegalStateException("Sheet hierarchy contains a cycle around " + current + ".");
                }
                String parent = parentBySheetName.get(current);
                if (parent != null) {
                    stack.push(parent);
                }
            }
        }
    }
}
