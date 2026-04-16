package customer.batchimportcat.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
// @Transactional(propagation = Propagation.REQUIRES_NEW)
public class BatchImportFileStatusService {
    private final BatchImportPersistenceService batchImportPersistenceService;

    public BatchImportFileStatusService(BatchImportPersistenceService batchImportPersistenceService) {
        this.batchImportPersistenceService = batchImportPersistenceService;
    }

    public void markQueued(String fileUUID, Long jobInstanceId) {
        updateFileExecutionInfo(fileUUID, jobInstanceId, "Q", "Queued", 2);
    }

    public void markRunning(String fileUUID, Long jobInstanceId) {
        updateFileExecutionInfo(fileUUID, jobInstanceId, "R", "Running", 2);
    }

    public void markSuccess(String fileUUID, Long jobInstanceId) {
        updateFileExecutionInfo(fileUUID, jobInstanceId, "S", "Success", 3);
    }

    public void markError(String fileUUID, Long jobInstanceId) {
        updateFileExecutionInfo(fileUUID, jobInstanceId, "E", "Error", 1);
    }

    private void updateFileExecutionInfo(String fileUUID, Long jobInstanceId, String status, String statusText,
            int criticality) {
        batchImportPersistenceService.updateFileExecutionInfo(fileUUID, jobInstanceId, status, statusText, criticality);
    }
}
