package customer.batchimportcat.processor.example.headeritemschedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import customer.batchimportcat.batch.dynamic.dto.DynamicRow;
import customer.batchimportcat.batch.dynamic.dto.DynamicTable;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessContext;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessPayload;
import customer.batchimportcat.batch.dynamic.types.DynamicImportConfiguration;

class ExampleHeaderItemScheduleBatchImportProcessorTest {
    @Test
    void processSummarizesSchedulesAndWarnings() {
        ExampleHeaderItemScheduleBatchImportProcessor processor = new ExampleHeaderItemScheduleBatchImportProcessor();

        DynamicTable headers = new DynamicTable("header-uuid", "Header");
        DynamicRow header = new DynamicRow("header-uuid", "Header", 1);
        header.put("HeaderId", "H001");

        DynamicTable items = new DynamicTable("item-uuid", "Item");
        DynamicRow itemWithSchedule = new DynamicRow("item-uuid", "Item", 2);
        itemWithSchedule.put("ItemNo", "10");
        DynamicTable schedules = new DynamicTable("schedule-uuid", "Schedule");
        schedules.add(new DynamicRow("schedule-uuid", "Schedule", 3));
        itemWithSchedule.put("Schedule", schedules);

        DynamicRow itemWithoutSchedule = new DynamicRow("item-uuid", "Item", 4);
        itemWithoutSchedule.put("ItemNo", "20");
        itemWithoutSchedule.put("Schedule", new DynamicTable("schedule-uuid", "Schedule"));

        items.add(itemWithSchedule);
        items.add(itemWithoutSchedule);
        header.put("Item", items);
        headers.add(header);

        BatchImportProcessPayload payload = new BatchImportProcessPayload(
                Map.of("header-uuid", headers),
                Map.of("Header", headers));

        var result = processor.process(processContext(), payload);

        assertFalse(result.hasErrors());
        assertEquals(2, result.getMessages().size());
        assertEquals("W", result.getMessages().get(0).type());
        assertEquals("S", result.getMessages().get(1).type());
        assertEquals(4L, result.getMessages().get(0).line());
        assertEquals(1L, result.getMessages().get(1).line());
    }

    private BatchImportProcessContext processContext() {
        DynamicImportConfiguration configuration = new DynamicImportConfiguration(
                "config-1",
                "OBJ",
                "Object",
                "EXAMPLE_HEADER_ITEM_SCHEDULE",
                ExampleHeaderItemScheduleBatchImportProcessor.class.getName(),
                null,
                null,
                1,
                "A",
                List.of());
        return new BatchImportProcessContext("file-1", configuration, Map.of(), Map.of(), null);
    }
}
