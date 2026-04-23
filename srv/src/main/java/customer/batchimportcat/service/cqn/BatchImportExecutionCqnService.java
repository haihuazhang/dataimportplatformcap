package customer.batchimportcat.service.cqn;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sap.cds.Result;
// import com.sap.cds.Row;
import com.sap.cds.ql.Insert;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.CqnUpdate;

import cds.gen.dataimportservice.BatchImportExecution;
import cds.gen.dataimportservice.BatchImportExecution_;
import cds.gen.dataimportservice.DataImportService;
import customer.batchimportcat.batch.runtime.TaskState;
import customer.batchimportcat.model.BatchImportFileLaunchContext;
import customer.batchimportcat.model.ProcessorArtifactBinding;
import customer.batchimportcat.model.TaskLaunchResult;

@Service
public class BatchImportExecutionCqnService {
    private final DataImportService dataImportService;

    public BatchImportExecutionCqnService( DataImportService dataImportService) {
        this.dataImportService = dataImportService;
    }

    public String createSubmittedExecution(BatchImportFileLaunchContext fileContext, ProcessorArtifactBinding artifact,
            String taskHostApp) {
        BatchImportExecution execution = BatchImportExecution.create();
        execution.setId(UUID.randomUUID().toString());
        execution.setFileUUID(fileContext.fileUUID());
        execution.setConfigUUID(fileContext.configUUID());
        execution.setProcessKey(fileContext.processKey());
        execution.setProcessorArtifactID(artifact.id());
        execution.setProcessorVersion(artifact.version());
        execution.setArtifactChecksum(artifact.artifactChecksum());
        execution.setTaskHostApp(taskHostApp);
        execution.setTaskState(TaskState.SUBMITTED.value());

        dataImportService.run(Insert.into(BatchImportExecution_.class).entry(execution));
        return execution.getId();
    }

    public void markLaunchAccepted(String executionUUID, TaskLaunchResult launchResult) {
        Map<String, Object> data = new HashMap<>();
        data.put("TaskState", TaskState.SUBMITTED.value());
        data.put("TaskId", launchResult.platformTaskId());
        data.put("TaskName", launchResult.platformTaskName());
        data.put("FailureReason", null);

        CqnUpdate update = Update.entity(BatchImportExecution_.class)
                .data(data)
                .where(execution -> execution.ID().eq(executionUUID));
        dataImportService.run(update);
    }

    public void markLaunchFailed(String executionUUID, String failureReason) {
        Map<String, Object> data = new HashMap<>();
        data.put("TaskState", TaskState.FAILED.value());
        data.put("FailureReason", failureReason);
        data.put("FinishedAt", Instant.now());

        CqnUpdate update = Update.entity(BatchImportExecution_.class)
                .data(data)
                .where(execution -> execution.ID().eq(executionUUID));
        dataImportService.run(update);
    }

    public String loadRequiredFileUUID(String executionUUID) {
        Result result = dataImportService.run(
                Select.from(BatchImportExecution_.class).where(execution -> execution.ID().eq(executionUUID)));
        if (result.rowCount() <= 0) {
            throw new IllegalStateException("Batch import execution " + executionUUID + " was not found.");
        }
        BatchImportExecution row = result.single(BatchImportExecution.class);
        return row.getFileUUID();
    }
}
