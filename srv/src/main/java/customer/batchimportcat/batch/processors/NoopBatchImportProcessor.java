package customer.batchimportcat.batch.processors;

import org.springframework.stereotype.Component;

import customer.batchimportcat.batch.dynamic.dto.DynamicRow;
import customer.batchimportcat.batch.dynamic.dto.DynamicTable;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessContext;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessPayload;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessResult;

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
