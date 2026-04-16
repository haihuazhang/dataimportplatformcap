package customer.batchimportcat.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.sap.cds.Result;
import com.sap.cds.ResultBuilder;
import com.sap.cds.ql.cqn.CqnPredicate;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.changeset.ChangeSetContext;
import com.sap.cds.services.changeset.ChangeSetListener;
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
import customer.batchimportcat.batch.dynamic.types.DynamicFieldType;
import customer.batchimportcat.batch.processors.BatchImportProcessorRegistry;
import customer.batchimportcat.service.BatchImportJobTriggerService;
import customer.batchimportcat.utils.CheckDataVisitor;
import customer.batchimportcat.utils.UnmanagedReportUtils;

@Component
@ServiceName(DataImportService_.CDS_NAME)
public class DataImportServiceHandler implements EventHandler {
    private final CdsModel model;
    private final BatchImportProcessorRegistry processorRegistry;
    private final BatchImportJobTriggerService batchImportJobTriggerService;

    public DataImportServiceHandler(CdsModel model,
            BatchImportProcessorRegistry processorRegistry,
            BatchImportJobTriggerService batchImportJobTriggerService) {
        this.model = model;
        this.processorRegistry = processorRegistry;
        this.batchImportJobTriggerService = batchImportJobTriggerService;
    }

    @After(event = CqnService.EVENT_CREATE, entity = BatchImportFile_.CDS_NAME)
    public void callBatchJob(Stream<BatchImportFile> batchImportFiles, CdsCreateEventContext context) {
        List<String> fileUUIDs = batchImportFiles
                .filter(file -> !Boolean.FALSE.equals(file.getIsActiveEntity()))
                .map(BatchImportFile::getId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        if (fileUUIDs.isEmpty()) {
            return;
        }

        ChangeSetContext changeSetContext = context.getChangeSetContext();
        if (changeSetContext == null) {
            fileUUIDs.forEach(batchImportJobTriggerService::trigger);
            return;
        }

        changeSetContext.register(new ChangeSetListener() {
            @Override
            public void afterClose(boolean completed) {
                if (completed) {
                    fileUUIDs.forEach(batchImportJobTriggerService::trigger);
                }
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

    private Result buildResult(CqnSelect select, List<? extends Map<String, ?>> rows) {
        List<? extends Map<String, ?>> filteredRows = new ArrayList<>(rows);
        UnmanagedReportUtils.sort(select.orderBy(), filteredRows);
        long inlineCount = filteredRows.size();
        List<? extends Map<String, ?>> pagedRows = UnmanagedReportUtils.getTopSkip(select.top(), select.skip(),
                filteredRows);
        return ResultBuilder.selectedRows(pagedRows).inlineCount(inlineCount).result();
    }
}
