package customer.batchimportcat.batch.dynamic;

public interface BatchImportProcessor {
    String getProcessKey();

    default String getDescription() {
        return getProcessKey();
    }

    BatchImportProcessResult process(BatchImportProcessContext context, BatchImportProcessPayload payload);
}
