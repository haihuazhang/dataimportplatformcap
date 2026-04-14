package customer.batchimportcat.batch.processors;

import org.springframework.stereotype.Component;

import customer.batchimportcat.batch.dynamic.dto.BatchImportProcessResult;
import customer.batchimportcat.batch.dynamic.dto.DynamicRow;
import customer.batchimportcat.batch.dynamic.dto.DynamicTable;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessContext;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessPayload;

@Component
public class ExampleHeaderItemScheduleBatchImportProcessor implements BatchImportProcessor {
    private static final String ROOT_STRUCTURE_NAME = "Header";
    private static final String ITEM_STRUCTURE_NAME = "Item";
    private static final String SCHEDULE_STRUCTURE_NAME = "Schedule";

    @Override
    public String getProcessKey() {
        return "EXAMPLE_HEADER_ITEM_SCHEDULE";
    }

    @Override
    public String getDescription() {
        return "Example multi-level processor for Header -> Item -> Schedule payloads";
    }

    @Override
    public BatchImportProcessResult process(BatchImportProcessContext context, BatchImportProcessPayload payload) {
        BatchImportProcessResult result = new BatchImportProcessResult();

        DynamicTable headers;
        try {
            headers = payload.getRequiredRootTableByName(ROOT_STRUCTURE_NAME);
        } catch (IllegalArgumentException exception) {
            result.addError(0, "MISSING_ROOT_STRUCTURE",
                    "Root structure Header is required for EXAMPLE_HEADER_ITEM_SCHEDULE.",
                    exception.getMessage());
            return result;
        }

        for (DynamicRow header : headers) {
            try {
                DynamicTable items = header.getRequiredChildTable(ITEM_STRUCTURE_NAME);
                int scheduleCount = 0;

                for (DynamicRow item : items) {
                    DynamicTable schedules = item.getRequiredChildTable(SCHEDULE_STRUCTURE_NAME);
                    scheduleCount += schedules.size();

                    if (schedules.isEmpty()) {
                        result.addWarning(item.getLineNumber(),
                                "Item " + businessKey(item, "ItemNo", "item_no", "ItemId", "item_id")
                                        + " has no schedules.");
                    }
                }

                result.addSuccess(header.getLineNumber(),
                        "Header " + businessKey(header, "HeaderId", "header_id", "DocumentNo", "document_no")
                                + " contains " + items.size() + " items and " + scheduleCount + " schedules.");
            } catch (Exception exception) {
                result.addError(header.getLineNumber(), "MULTI_LEVEL_PROCESSING_EXCEPTION",
                        "Failed to process Header -> Item -> Schedule hierarchy.", exception.getMessage());
            }
        }
        return result;
    }

    private String businessKey(DynamicRow row, String... candidateFields) {
        for (String fieldName : candidateFields) {
            Object value = row.get(fieldName);
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value);
            }
        }
        return row.getStructureName() + "#" + row.getLineNumber();
    }
}
