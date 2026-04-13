package customer.batchimportcat.batch.dynamic;

import java.util.List;

public interface BatchImportProcessor {
    String getProcessKey();

    default String getDescription() {
        return getProcessKey();
    }

    BatchImportProcessResult process(BatchImportProcessContext context, List<DynamicNode> items);
}
