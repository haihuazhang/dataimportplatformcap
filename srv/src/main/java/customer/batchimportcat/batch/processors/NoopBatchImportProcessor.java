package customer.batchimportcat.batch.processors;

import org.springframework.stereotype.Component;

import customer.batchimportcat.batch.dynamic.BatchImportProcessContext;
import customer.batchimportcat.batch.dynamic.BatchImportProcessPayload;
import customer.batchimportcat.batch.dynamic.BatchImportProcessResult;
import customer.batchimportcat.batch.dynamic.BatchImportProcessor;
import customer.batchimportcat.batch.dynamic.DynamicRow;
import customer.batchimportcat.batch.dynamic.DynamicTable;

@Component
public class NoopBatchImportProcessor implements BatchImportProcessor {
    @Override
    public String getProcessKey() {
        return "NOOP";
    }

    @Override
    public String getDescription() {
        return "Validate and store original rows without business writeback";
    }

    @Override
    public BatchImportProcessResult process(BatchImportProcessContext context, BatchImportProcessPayload payload) {
        BatchImportProcessResult result = new BatchImportProcessResult();
        for (DynamicTable rootTable : payload.rootTables()) {
            for (DynamicRow row : rootTable) {
                result.addSuccess(row.getLineNumber(),
                        "NOOP processor accepted root row from structure " + row.getStructureName() + ".");
            }
        }
        return result;
    }
}
