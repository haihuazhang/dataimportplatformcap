package customer.batchimportcat.service.cqn.impl;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.CqnUpdate;
import com.sap.cds.services.persistence.PersistenceService;

import customer.batchimportcat.batch.runtime.TaskState;
import customer.batchimportcat.service.cqn.TaskExecutionPersistenceService;

@Service
public class TaskExecutionDBCqnService implements TaskExecutionPersistenceService {
    private static final String EXECUTION_ENTITY = "zzdt_BatchImportExecution";

    private final PersistenceService db;

    public TaskExecutionDBCqnService(PersistenceService db) {
        this.db = db;
    }

    @Override
    public void markRunning(String executionUUID) {
        Map<String, Object> data = new HashMap<>();
        data.put("TaskState", TaskState.RUNNING.value());
        data.put("StartedAt", Instant.now());
        data.put("FailureReason", null);
        update(executionUUID, data);
    }

    @Override
    public void recordJobInstance(String executionUUID, Long jobInstanceId) {
        if (jobInstanceId == null) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("JobInstanceId", String.valueOf(jobInstanceId));
        update(executionUUID, data);
    }

    @Override
    public void markSucceeded(String executionUUID, Long jobInstanceId) {
        Map<String, Object> data = new HashMap<>();
        data.put("TaskState", TaskState.SUCCEEDED.value());
        data.put("FinishedAt", Instant.now());
        data.put("FailureReason", null);
        if (jobInstanceId != null) {
            data.put("JobInstanceId", String.valueOf(jobInstanceId));
        }
        update(executionUUID, data);
    }

    @Override
    public void markFailed(String executionUUID, Long jobInstanceId, String failureReason) {
        Map<String, Object> data = new HashMap<>();
        data.put("TaskState", TaskState.FAILED.value());
        data.put("FinishedAt", Instant.now());
        data.put("FailureReason", failureReason);
        if (jobInstanceId != null) {
            data.put("JobInstanceId", String.valueOf(jobInstanceId));
        }
        update(executionUUID, data);
    }

    private void update(String executionUUID, Map<String, Object> data) {
        CqnUpdate update = Update.entity(EXECUTION_ENTITY)
                .data(data)
                .byId(executionUUID);
        db.run(update);
    }
}
