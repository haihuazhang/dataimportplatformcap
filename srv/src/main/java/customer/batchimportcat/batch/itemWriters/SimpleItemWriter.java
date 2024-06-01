package customer.batchimportcat.batch.itemWriters;

import java.util.Map;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.sap.cds.Struct;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.services.cds.CqnService;

import cds.gen.Zzsdtimp001;
import cds.gen.dataimportservice.DataImportService_;

public class SimpleItemWriter<T> implements ItemWriter<T> {
    @Autowired
    CdsModel cdsModel;

    @Autowired
    @Qualifier(DataImportService_.CDS_NAME)
    CqnService dataimportService; 

    @Override
    public void write(Chunk<? extends T> chunk) throws Exception {
        // TODO Auto-generated method stub
        chunk.getItems().forEach(entry -> {
            Zzsdtimp001 dtimp001 = Struct.access((Map<String, Object>) entry).as(Zzsdtimp001.class);
            System.out.println(dtimp001.getFieldStr01());
            System.out.println(dtimp001.getFieldStr02());
            System.out.println(dtimp001.getFieldDec01());
        });
    }
}
