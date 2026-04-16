package customer.batchimportcat.batch.listeners;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import customer.batchimportcat.consts.Constant;
import customer.batchimportcat.service.BatchImportFileStatusService;

@Component
public class BatchImportJobExecutionListener implements JobExecutionListener {
    private final BatchImportFileStatusService batchImportFileStatusService;

    public BatchImportJobExecutionListener(BatchImportFileStatusService batchImportFileStatusService) {
        this.batchImportFileStatusService = batchImportFileStatusService;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        String fileUUID = jobExecution.getJobParameters().getString("fileUUID");
        if (fileUUID == null || fileUUID.isBlank()) {
            return;
        }
        batchImportFileStatusService.markRunning(fileUUID, jobExecution.getJobId());
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
