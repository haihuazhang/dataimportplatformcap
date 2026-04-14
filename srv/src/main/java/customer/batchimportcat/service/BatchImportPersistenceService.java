package customer.batchimportcat.service;

import java.util.List;

import customer.batchimportcat.batch.dynamic.types.BatchImportOriginalDataRecord;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessMessage;

public interface BatchImportPersistenceService {
    void saveOriginalData(String fileUUID, List<BatchImportOriginalDataRecord> entries);

    void saveMessages(String fileUUID, List<BatchImportProcessMessage> entries);
}
