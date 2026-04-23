package customer.batchimportcat.batch.listeners;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import customer.batchimportcat.consts.Constant;
import customer.batchimportcat.service.BatchImportFileStatusService;
import customer.batchimportcat.service.cqn.TaskExecutionPersistenceService;

@Component
public class BatchImportJobExecutionListener implements JobExecutionListener {
    private final BatchImportFileStatusService batchImportFileStatusService;
    private final TaskExecutionPersistenceService taskExecutionLifecycleService;

    public BatchImportJobExecutionListener(BatchImportFileStatusService batchImportFileStatusService,
            TaskExecutionPersistenceService taskExecutionLifecycleService) {
        this.batchImportFileStatusService = batchImportFileStatusService;
        this.taskExecutionLifecycleService = taskExecutionLifecycleService;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        String fileUUID = jobExecution.getJobParameters().getString("fileUUID");
        String executionUUID = jobExecution.getJobParameters().getString("executionUUID");
        if (fileUUID == null || fileUUID.isBlank()) {
            return;
        }
        batchImportFileStatusService.markRunning(fileUUID, jobExecution.getJobId());
        if (executionUUID != null && !executionUUID.isBlank()) {
            taskExecutionLifecycleService.recordJobInstance(executionUUID, jobExecution.getJobId());
        }
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String fileUUID = jobExecution.getJobParameters().getString("fileUUID");
        if (fileUUID == null || fileUUID.isBlank()) {
            return;
        }

        boolean hasProcessingErrors = Boolean.TRUE.equals(
                jobExecution.getExecutionContext().get(Constant.HAS_PROCESSING_ERRORS));
        if (jobExecution.getStatus().isUnsuccessful() || hasProcessingErrors) {
            batchImportFileStatusService.markError(fileUUID, jobExecution.getJobId());
            return;
        }

        batchImportFileStatusService.markSuccess(fileUUID, jobExecution.getJobId());
    }
}
