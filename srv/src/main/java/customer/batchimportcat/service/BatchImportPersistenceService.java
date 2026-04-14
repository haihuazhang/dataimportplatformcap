package customer.batchimportcat.service;

import java.util.List;

import customer.batchimportcat.batch.dynamic.types.BatchImportConfigData;
import customer.batchimportcat.batch.dynamic.types.BatchImportOriginalDataRecord;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessMessage;

public interface BatchImportPersistenceService {
    byte[] loadFileContent(String fileUUID);

    BatchImportConfigData loadConfigData(String fileUUID);

    void saveOriginalData(String fileUUID, List<BatchImportOriginalDataRecord> entries);

    void saveMessages(String fileUUID, List<BatchImportProcessMessage> entries);

    void updateFileExecutionInfo(String fileUUID, Long jobInstanceId, String status, String statusText,
            int criticality);
}
