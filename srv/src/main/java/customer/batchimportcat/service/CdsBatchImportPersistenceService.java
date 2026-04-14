package customer.batchimportcat.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.sap.cds.ql.Insert;
import com.sap.cds.ql.cqn.CqnInsert;
import com.sap.cds.services.cds.CqnService;

import cds.gen.dataimportservice.BatchImportData;
import cds.gen.dataimportservice.BatchImportData_;
import cds.gen.dataimportservice.BatchImportMessage;
import cds.gen.dataimportservice.BatchImportMessage_;
import cds.gen.dataimportservice.DataImportService_;
import customer.batchimportcat.batch.dynamic.types.BatchImportOriginalDataRecord;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessMessage;

@Service
public class CdsBatchImportPersistenceService implements BatchImportPersistenceService {
    private final CqnService dataImportService;

    public CdsBatchImportPersistenceService(@Qualifier(DataImportService_.CDS_NAME) CqnService dataImportService) {
        this.dataImportService = dataImportService;
    }

    @Override
    public void saveOriginalData(String fileUUID, List<BatchImportOriginalDataRecord> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }

        List<BatchImportData> dataEntries = new ArrayList<>(entries.size());
        for (BatchImportOriginalDataRecord entry : entries) {
            BatchImportData data = BatchImportData.create();
            data.setFileUUID(fileUUID);
            data.setLine(entry.line());
            data.setStructureName(entry.structureName());
            data.setDataJson(entry.dataJson());
            dataEntries.add(data);
        }

        CqnInsert insert = Insert.into(BatchImportData_.class).entries(dataEntries);
        dataImportService.run(insert);
    }

    @Override
    public void saveMessages(String fileUUID, List<BatchImportProcessMessage> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }

        List<BatchImportMessage> messageEntries = new ArrayList<>(entries.size());
        for (BatchImportProcessMessage entry : entries) {
            BatchImportMessage message = BatchImportMessage.create();
            message.setFileUUID(fileUUID);
            message.setLine(entry.line());
            message.setType(entry.type());
            message.setCode(entry.code());
            message.setMessage(entry.message());
            message.setDetails(entry.details());
            messageEntries.add(message);
        }

        CqnInsert insert = Insert.into(BatchImportMessage_.class).entries(messageEntries);
        dataImportService.run(insert);
    }
}
