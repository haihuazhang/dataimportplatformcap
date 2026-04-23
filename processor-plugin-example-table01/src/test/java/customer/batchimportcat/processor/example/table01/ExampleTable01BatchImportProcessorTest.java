package customer.batchimportcat.processor.example.table01;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sap.cds.Result;
import com.sap.cds.ql.cqn.CqnInsert;
import com.sap.cds.services.cds.CqnService;

import customer.batchimportcat.batch.dynamic.dto.DynamicRow;
import customer.batchimportcat.batch.dynamic.dto.DynamicTable;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessContext;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessPayload;
import customer.batchimportcat.batch.dynamic.types.DynamicImportConfiguration;
import customer.batchimportcat.batch.runtime.ProcessorRuntime;

class ExampleTable01BatchImportProcessorTest {
    @Test
    void processWritesEachRootRowThroughRuntimeCqnService() {
        ExampleTable01BatchImportProcessor processor = new ExampleTable01BatchImportProcessor();

        CqnService exampleService = mock(CqnService.class);
        Result insertResult = mock(Result.class);
        when(insertResult.rowCount()).thenReturn(1L);
        when(exampleService.run(any(CqnInsert.class))).thenReturn(insertResult);

        ProcessorRuntime runtime = mock(ProcessorRuntime.class);
        when(runtime.cqnService("ExampleService")).thenReturn(exampleService);

        DynamicTable rootTable = new DynamicTable("root-uuid", "Root");
        DynamicRow row = new DynamicRow("root-uuid", "Root", 1);
        row.put("field_str01", "A");
        row.put("field_str02", "B");
        row.put("field_dec_01", "12.300");
        rootTable.add(row);

        BatchImportProcessPayload payload = new BatchImportProcessPayload(
                Map.of("root-uuid", rootTable),
                Map.of("Root", rootTable));

        var result = processor.process(processContext(runtime), payload);

        assertFalse(result.hasErrors());
        assertEquals(1, result.getMessages().size());
        assertEquals("S", result.getMessages().get(0).type());
        assertEquals(1L, result.getMessages().get(0).line());
    }

    private BatchImportProcessContext processContext(ProcessorRuntime runtime) {
        DynamicImportConfiguration configuration = new DynamicImportConfiguration(
                "config-1",
                "OBJ",
                "Object",
                "EXAMPLE_TABLE01",
                ExampleTable01BatchImportProcessor.class.getName(),
                null,
                null,
                1,
                "A",
                List.of());
        return new BatchImportProcessContext("file-1", configuration, Map.of(), Map.of(), runtime);
    }
}
