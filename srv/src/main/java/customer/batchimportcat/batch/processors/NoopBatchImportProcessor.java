package customer.batchimportcat.batch.processors;

import java.util.List;

import org.springframework.stereotype.Component;

import customer.batchimportcat.batch.dynamic.BatchImportProcessContext;
import customer.batchimportcat.batch.dynamic.BatchImportProcessResult;
import customer.batchimportcat.batch.dynamic.BatchImportProcessor;
import customer.batchimportcat.batch.dynamic.DynamicNode;

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
    public BatchImportProcessResult process(BatchImportProcessContext context, List<DynamicNode> items) {
        BatchImportProcessResult result = new BatchImportProcessResult();
        for (DynamicNode item : items) {
            result.addSuccess(item.getLineNumber(),
                    "NOOP processor accepted root node from structure " + item.getStructureName() + ".");
        }
        return result;
    }
}
