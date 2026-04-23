package customer.batchimportcat.processor.noop;

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

class NoopBatchImportProcessorTest {
    @Test
    void processAcceptsEachRootRow() {
        NoopBatchImportProcessor processor = new NoopBatchImportProcessor();
        DynamicTable rootTable = new DynamicTable("root-uuid", "Root");
        rootTable.add(new DynamicRow("root-uuid", "Root", 1));
        rootTable.add(new DynamicRow("root-uuid", "Root", 2));

        BatchImportProcessPayload payload = new BatchImportProcessPayload(
                Map.of("root-uuid", rootTable),
                Map.of("Root", rootTable));

        var result = processor.process(processContext(), payload);

        assertFalse(result.hasErrors());
        assertEquals(2, result.getMessages().size());
        assertEquals("S", result.getMessages().get(0).type());
        assertEquals(1L, result.getMessages().get(0).line());
        assertEquals(2L, result.getMessages().get(1).line());
    }

    private BatchImportProcessContext processContext() {
        DynamicImportConfiguration configuration = new DynamicImportConfiguration(
                "config-1",
                "OBJ",
                "Object",
                "NOOP",
                NoopBatchImportProcessor.class.getName(),
                null,
                null,
                1,
                "A",
                List.of());
        return new BatchImportProcessContext("file-1", configuration, Map.of(), Map.of(), null);
    }
}
