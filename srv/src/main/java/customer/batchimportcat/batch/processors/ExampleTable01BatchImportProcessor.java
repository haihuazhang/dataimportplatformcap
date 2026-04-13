package customer.batchimportcat.batch.processors;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sap.cds.Result;
import com.sap.cds.ql.Insert;
import com.sap.cds.ql.cqn.CqnInsert;
import com.sap.cds.services.cds.CqnService;

import cds.gen.exampleservice.ExampleService_;
import cds.gen.exampleservice.ZZTable01;
import cds.gen.exampleservice.ZZTable01_;
import customer.batchimportcat.batch.dynamic.BatchImportProcessContext;
import customer.batchimportcat.batch.dynamic.BatchImportProcessResult;
import customer.batchimportcat.batch.dynamic.BatchImportProcessor;
import customer.batchimportcat.batch.dynamic.DynamicNode;

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
    public BatchImportProcessResult process(BatchImportProcessContext context, List<DynamicNode> items) {
        BatchImportProcessResult result = new BatchImportProcessResult();
        for (DynamicNode item : items) {
            try {
                ZZTable01 entry = ZZTable01.create();
                entry.setFieldStr01(asString(item.getFields().get("field_str01")));
                entry.setFieldStr02(asString(item.getFields().get("field_str02")));
                entry.setFieldDec01(asBigDecimal(item.getFields().get("field_dec_01")));

                CqnInsert insert = Insert.into(ZZTable01_.class).entry(entry);
                Result insertResult = exampleService.run(insert);
                if (insertResult.rowCount() > 0) {
                    result.addSuccess(item.getLineNumber(), "Row was written to ExampleService.ZZTable01.");
                } else {
                    result.addError(item.getLineNumber(), "WRITE_FAILED",
                            "Write Record to ExampleService.ZZTable01 not success.",
                            context.configuration().object());
                }
            } catch (Exception exception) {
                result.addError(item.getLineNumber(), "PROCESSING_EXCEPTION",
                        "Failed to process row for ExampleService.ZZTable01.", exception.getMessage());
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
