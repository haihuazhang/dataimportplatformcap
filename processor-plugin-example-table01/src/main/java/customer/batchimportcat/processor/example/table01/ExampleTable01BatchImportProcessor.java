package customer.batchimportcat.processor.example.table01;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sap.cds.Result;
import com.sap.cds.ql.Insert;
import com.sap.cds.ql.cqn.CqnInsert;
import com.sap.cds.services.cds.CqnService;

import customer.batchimportcat.batch.dynamic.dto.DynamicRow;
import customer.batchimportcat.batch.dynamic.dto.DynamicTable;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessContext;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessPayload;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessResult;
import customer.batchimportcat.batch.processors.BatchImportProcessor;
import customer.batchimportcat.batch.runtime.ProcessorRuntime;

public class ExampleTable01BatchImportProcessor implements BatchImportProcessor {
    private static final String EXAMPLE_SERVICE_NAME = "ExampleService";
    private static final String EXAMPLE_ENTITY_NAME = "ExampleService.ZZTable01";

    @Override
    public String getProcessKey() {
        return "EXAMPLE_TABLE01";
    }

    @Override
    public String getDescription() {
        return "Write root rows into ExampleService.ZZTable01";
    }

    @Override
    public BatchImportProcessResult process(BatchImportProcessContext context, BatchImportProcessPayload payload) {
        BatchImportProcessResult result = new BatchImportProcessResult();
        CqnService exampleService = resolveExampleService(context);
        for (DynamicTable rootTable : payload.rootTables()) {
            for (DynamicRow row : rootTable) {
                try {
                    CqnInsert insert = Insert.into(EXAMPLE_ENTITY_NAME).entry(toEntry(row));
                    Result insertResult = exampleService.run(insert);
                    if (insertResult.rowCount() > 0) {
                        result.addSuccess(row.getLineNumber(), "Row was written to ExampleService.ZZTable01.");
                    } else {
                        result.addError(row.getLineNumber(), "WRITE_FAILED",
                                "Write record to ExampleService.ZZTable01 was not successful.",
                                context.configuration().object());
                    }
                } catch (Exception exception) {
                    result.addError(row.getLineNumber(), "PROCESSING_EXCEPTION",
                            "Failed to process row for ExampleService.ZZTable01.", exception.getMessage());
                }
            }
        }
        return result;
    }

    private CqnService resolveExampleService(BatchImportProcessContext context) {
        ProcessorRuntime runtime = context.runtime();
        if (runtime == null) {
            throw new IllegalStateException("Processor runtime is required for EXAMPLE_TABLE01.");
        }
        return runtime.cqnService(EXAMPLE_SERVICE_NAME);
    }

    private Map<String, Object> toEntry(DynamicRow row) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("field_str01", asString(row.get("field_str01")));
        entry.put("field_str02", asString(row.get("field_str02")));
        entry.put("field_dec_01", asBigDecimal(row.get("field_dec_01")));
        return entry;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(String.valueOf(value));
    }
}
