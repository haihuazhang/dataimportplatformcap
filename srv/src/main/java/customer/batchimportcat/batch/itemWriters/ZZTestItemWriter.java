package customer.batchimportcat.batch.itemWriters;

import java.util.Map;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.sap.cds.Result;
import com.sap.cds.Struct;
import com.sap.cds.ql.Insert;
import com.sap.cds.ql.cqn.CqnInsert;
import com.sap.cds.services.cds.CqnService;

import cds.gen.Zzsdtimp001;
import cds.gen.dataimportservice.DataImportService_;
import cds.gen.exampleservice.ExampleService_;
import cds.gen.exampleservice.ZZTable01;
import cds.gen.exampleservice.ZZTable01_;
import customer.batchimportcat.batch.exceptions.BatchExceptionsUtil;

public class ZZTestItemWriter<T> implements ItemWriter<T> {
    @Autowired
    @Qualifier(ExampleService_.CDS_NAME)
    CqnService testService;

    @Override
    public void write(Chunk<? extends T> chunk) throws Exception {
        // TODO Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'write'");

        chunk.getItems().forEach((entry) -> {
            Zzsdtimp001 dtimp001 = Struct.access((Map<String, Object>) entry).as(Zzsdtimp001.class);

            ZZTable01 table01 = ZZTable01.create();
            table01.setFieldStr01(dtimp001.getFieldStr01());
            table01.setFieldStr02(dtimp001.getFieldStr02());
            table01.setFieldDec01(dtimp001.getFieldDec01());

            CqnInsert insert = Insert.into(ZZTable01_.class).entry(table01);
            Result result = testService.run(insert);
            if (result.rowCount() > 0) {
                
            } else {
                throw BatchExceptionsUtil.getBatchRecordNotSucess(ZZTable01_.CDS_NAME, 0);
            }

        });

    }

}
