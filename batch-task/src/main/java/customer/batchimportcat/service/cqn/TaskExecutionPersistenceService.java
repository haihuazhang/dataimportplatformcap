package customer.batchimportcat.service.cqn;

public interface TaskExecutionPersistenceService {
    void markRunning(String executionUUID);

    void recordJobInstance(String executionUUID, Long jobInstanceId);

    void markSucceeded(String executionUUID, Long jobInstanceId);

    void markFailed(String executionUUID, Long jobInstanceId, String failureReason);
}
