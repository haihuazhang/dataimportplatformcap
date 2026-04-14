package customer.batchimportcat.batch.processors;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sap.cds.Result;
import com.sap.cds.ql.Insert;
import com.sap.cds.ql.cqn.CqnInsert;
import com.sap.cds.services.cds.CqnService;

import cds.gen.exampleservice.ExampleService_;
import cds.gen.exampleservice.ZZTable01;
import cds.gen.exampleservice.ZZTable01_;
import customer.batchimportcat.batch.dynamic.dto.BatchImportProcessResult;
import customer.batchimportcat.batch.dynamic.dto.DynamicRow;
import customer.batchimportcat.batch.dynamic.dto.DynamicTable;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessContext;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessPayload;

@Component
public class ExampleTable01BatchImportProcessor implements BatchImportProcessor {
    @Qualifier(ExampleService_.CDS_NAME)
    private final CqnService exampleService;

    public ExampleTable01BatchImportProcessor(@Qualifier(ExampleService_.CDS_NAME) CqnService exampleService) {
        this.exampleService = exampleService;
    }

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
        for (DynamicTable rootTable : payload.rootTables()) {
            for (DynamicRow row : rootTable) {
                try {
                    ZZTable01 entry = ZZTable01.create();
                    entry.setFieldStr01(asString(row.get("field_str01")));
                    entry.setFieldStr02(asString(row.get("field_str02")));
                    entry.setFieldDec01(asBigDecimal(row.get("field_dec_01")));

                    CqnInsert insert = Insert.into(ZZTable01_.class).entry(entry);
                    Result insertResult = exampleService.run(insert);
                    if (insertResult.rowCount() > 0) {
                        result.addSuccess(row.getLineNumber(), "Row was written to ExampleService.ZZTable01.");
                    } else {
                        result.addError(row.getLineNumber(), "WRITE_FAILED",
                                "Write Record to ExampleService.ZZTable01 not success.",
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
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(String.valueOf(value));
    }
}
