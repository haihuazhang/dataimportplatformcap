package customer.batchimportcat.batch.tasklets;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import customer.batchimportcat.batch.dynamic.DynamicConfigurationBuilder;
import customer.batchimportcat.batch.dynamic.types.BatchImportConfigData;
import customer.batchimportcat.batch.dynamic.types.DynamicFieldDefinition;
import customer.batchimportcat.batch.dynamic.types.DynamicImportConfiguration;
import customer.batchimportcat.batch.dynamic.types.DynamicStructureDefinition;
import customer.batchimportcat.batch.exceptions.BatchExceptionsUtil;
import customer.batchimportcat.service.BatchImportPersistenceService;

public class GetBatchImportConfigTasklet implements Tasklet {
    private BatchImportPersistenceService batchImportPersistenceService;

    private DynamicConfigurationBuilder dynamicConfigurationBuilder;

    @org.springframework.beans.factory.annotation.Autowired
    public void setBatchImportPersistenceService(BatchImportPersistenceService batchImportPersistenceService) {
        this.batchImportPersistenceService = batchImportPersistenceService;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void setDynamicConfigurationBuilder(DynamicConfigurationBuilder dynamicConfigurationBuilder) {
        this.dynamicConfigurationBuilder = dynamicConfigurationBuilder;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        String fileUUID = String.valueOf(chunkContext.getStepContext().getJobParameters().get("fileUUID"));

        byte[] fileContent = batchImportPersistenceService.loadFileContent(fileUUID);
        BatchImportConfigData loadedData = batchImportPersistenceService.loadConfigData(fileUUID);

        Map<String, Serializable> configData = loadedData.configData();
        DynamicImportConfiguration dynamicConfig = dynamicConfigurationBuilder.build(configData,
                loadedData.structureRows(), loadedData.fieldRows());
        validate(dynamicConfig);

        contribution.getStepExecution().getJobExecution().getExecutionContext().put("fileContent", fileContent);
        contribution.getStepExecution().getJobExecution().getExecutionContext().put("configData", configData);
        contribution.getStepExecution().getJobExecution().getExecutionContext().put("dynamicConfig", dynamicConfig);

        return RepeatStatus.FINISHED;
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
