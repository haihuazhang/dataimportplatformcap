package customer.batchimportcat.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sap.cds.Result;
import com.sap.cds.ResultBuilder;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.CqnPredicate;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.CqnUpdate;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.reflect.CdsStructuredType;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

import cds.gen.dataimportservice.BatchImportFile;
import cds.gen.dataimportservice.BatchImportFile_;
import cds.gen.dataimportservice.DataImportService_;
import cds.gen.dataimportservice.ImportFieldType;
import cds.gen.dataimportservice.ImportFieldType_;
import cds.gen.dataimportservice.ImportStructure;
import cds.gen.dataimportservice.ImportStructure_;
import cds.gen.dataimportservice.ImplementedByClass_;
import cds.gen.dataimportservice.ProcessKeyValueHelp;
import cds.gen.dataimportservice.ProcessKeyValueHelp_;
import customer.batchimportcat.batch.dynamic.BatchImportProcessorRegistry;
import customer.batchimportcat.batch.dynamic.DynamicFieldType;
import customer.batchimportcat.utils.CheckDataVisitor;
import customer.batchimportcat.utils.UnmanagedReportUtils;

@Component
@ServiceName(DataImportService_.CDS_NAME)
public class DataImportServiceHandler implements EventHandler {
    @Qualifier("asyncJobLauncher")
    private final JobLauncher jobLauncher;

    @Qualifier("batchImportJob")
    private final Job job;

    private final CdsModel model;
    private final BatchImportProcessorRegistry processorRegistry;
    private final CqnService dataImportService;

    public DataImportServiceHandler(@Qualifier("asyncJobLauncher") JobLauncher jobLauncher,
            @Qualifier("batchImportJob") Job job,
            CdsModel model,
            BatchImportProcessorRegistry processorRegistry,
            @Qualifier(DataImportService_.CDS_NAME) CqnService dataImportService) {
        this.jobLauncher = jobLauncher;
        this.job = job;
        this.model = model;
        this.processorRegistry = processorRegistry;
        this.dataImportService = dataImportService;
    }

    @After(event = CqnService.EVENT_CREATE, entity = BatchImportFile_.CDS_NAME)
    public void callBatchJob(Stream<BatchImportFile> batchImportFiles) {
        batchImportFiles.forEach(file -> {
            if (Boolean.FALSE.equals(file.getIsActiveEntity())) {
                return;
            }

            Map<String, JobParameter<?>> parameters = new HashMap<>();
            parameters.put("fileUUID", new JobParameter<>(file.getId(), String.class));

            try {
                JobExecution jobExecution = jobLauncher.run(job, new JobParameters(parameters));
                updateFileExecutionInfo(file.getId(), jobExecution.getJobId().toString(), "Q", "Queued", 2);
            } catch (Exception exception) {
                updateFileExecutionInfo(file.getId(), null, "E", "Error", 1);
            }
        });
    }

    @On(event = CqnService.EVENT_READ, entity = ImportStructure_.CDS_NAME)
    public void getAllImportStructureVH(CdsReadEventContext context) {
        List<ImportStructure> importStructures = new ArrayList<>();
        CqnSelect select = context.getCqn();

        model.structuredTypes()
                .filter(com.sap.cds.reflect.CdsAnnotatable.byAnnotation("batchdataimport"))
                .forEach(structure -> {
                    ImportStructure importStructure = ImportStructure.create();
                    importStructure.setName(structure.getName());
                    importStructure.setDescription(structure.getAnnotationValue("title", null));

                    CheckDataVisitor visitor = new CheckDataVisitor(importStructure);
                    try {
                        CqnPredicate predicate = select.where().orElseThrow();
                        predicate.accept(visitor);
                        if (visitor.matches()) {
                            importStructures.add(importStructure);
                        }
                    } catch (Exception exception) {
                        importStructures.add(importStructure);
                    }
                });

        context.setResult(buildResult(select, importStructures));
    }

    @On(event = CqnService.EVENT_READ, entity = ProcessKeyValueHelp_.CDS_NAME)
    public void getAllProcessKeys(CdsReadEventContext context) {
        List<ProcessKeyValueHelp> rows = processorRegistry.getValueHelps().stream()
                .map(row -> {
                    ProcessKeyValueHelp entity = ProcessKeyValueHelp.create();
                    entity.setProcessKey(String.valueOf(row.get("ProcessKey")));
                    entity.setDescription(String.valueOf(row.get("Description")));
                    return entity;
                })
                .toList();
        context.setResult(buildResult(context.getCqn(), rows));
    }

    @On(event = CqnService.EVENT_READ, entity = ImportFieldType_.CDS_NAME)
    public void getAllFieldTypes(CdsReadEventContext context) {
        List<ImportFieldType> rows = new ArrayList<>();
        for (DynamicFieldType fieldType : DynamicFieldType.values()) {
            ImportFieldType entity = ImportFieldType.create();
            entity.setCode(fieldType.getCode());
            entity.setDescription(fieldType.getDescription());
            rows.add(entity);
        }
        context.setResult(buildResult(context.getCqn(), rows));
    }

    @On(event = CqnService.EVENT_READ, entity = ImplementedByClass_.CDS_NAME)
    public void getAllImplementedByClass(CdsReadEventContext context) throws IOException {
        context.setResult(buildResult(context.getCqn(), processorRegistry.getImplementedClasses()));
    }

    private void updateFileExecutionInfo(String fileUUID, String jobName, String status, String statusText,
            int criticality) {
        Map<String, Object> data = new HashMap<>();
        if (jobName != null) {
            data.put("JobName", jobName);
        }
        data.put("Status", status);
        data.put("StatusText", statusText);
        data.put("StatusCriticality", criticality);

        CqnUpdate update = Update.entity(BatchImportFile_.class)
                .data(data)
                .where(file -> file.ID().eq(fileUUID));
        dataImportService.run(update);
    }

    private Result buildResult(CqnSelect select, List<? extends Map<String, ?>> rows) {
        List<? extends Map<String, ?>> filteredRows = new ArrayList<>(rows);
        UnmanagedReportUtils.sort(select.orderBy(), filteredRows);
        long inlineCount = filteredRows.size();
        List<? extends Map<String, ?>> pagedRows = UnmanagedReportUtils.getTopSkip(select.top(), select.skip(),
                filteredRows);
        return ResultBuilder.selectedRows(pagedRows).inlineCount(inlineCount).result();
    }
}
