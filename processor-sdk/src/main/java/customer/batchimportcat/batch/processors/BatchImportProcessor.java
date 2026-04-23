package customer.batchimportcat.batch.processors;

import customer.batchimportcat.batch.dynamic.types.BatchImportProcessContext;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessPayload;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessResult;

public interface BatchImportProcessor {
    String getProcessKey();

    default String getDescription() {
        return getProcessKey();
    }

    BatchImportProcessResult process(BatchImportProcessContext context, BatchImportProcessPayload payload);
}
