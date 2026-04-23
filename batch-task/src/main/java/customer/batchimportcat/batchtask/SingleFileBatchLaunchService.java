package customer.batchimportcat.batchtask;

// import customer.batchimportcat.batch.configurations.BatchImportJobConfiguration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

// import com.sap.cds.services.utils.ErrorStatusException;

import customer.batchimportcat.consts.Constant;
import customer.batchimportcat.service.BatchImportFileStatusService;
import customer.batchimportcat.service.cqn.TaskExecutionPersistenceService;

@Service
public class SingleFileBatchLaunchService {
    // private final BatchImportJobConfiguration batchImportJobConfiguration;
    private final JobLauncher jobLauncher;
    private final Job batchImportJob;
    private final TaskExecutionPersistenceService taskExecutionLifecycleService;
    private final BatchImportFileStatusService batchImportFileStatusService;

    public SingleFileBatchLaunchService(@Qualifier("syncJobLauncher") JobLauncher jobLauncher,
            @Qualifier("batchImportJob") Job batchImportJob,
            TaskExecutionPersistenceService taskExecutionLifecycleService,
            BatchImportFileStatusService batchImportFileStatusService 
            // BatchImportJobConfiguration batchImportJobConfiguration
            ) {
        this.jobLauncher = jobLauncher;
        this.batchImportJob = batchImportJob;
        this.taskExecutionLifecycleService = taskExecutionLifecycleService;
        this.batchImportFileStatusService = batchImportFileStatusService;
        // this.batchImportJobConfiguration = batchImportJobConfiguration;
    }

    public boolean launch(String fileUUID, String executionUUID) {
        try {
            taskExecutionLifecycleService.markRunning(executionUUID);
            Map<String, JobParameter<?>> parameters = new HashMap<>();
            parameters.put("fileUUID", new JobParameter<>(fileUUID, String.class));
            parameters.put("executionUUID", new JobParameter<>(executionUUID, String.class));
            JobExecution jobExecution = jobLauncher.run(batchImportJob, new JobParameters(parameters));
            boolean hasProcessingErrors = Boolean.TRUE.equals(
                    jobExecution.getExecutionContext().get(Constant.HAS_PROCESSING_ERRORS));
            if (jobExecution.getStatus().isUnsuccessful() || hasProcessingErrors) {
                taskExecutionLifecycleService.markFailed(executionUUID, jobExecution.getJobId(),
                        "Batch import job finished unsuccessfully.");
                return false;
            }

            taskExecutionLifecycleService.markSucceeded(executionUUID, jobExecution.getJobId());
            return true;
        } catch (Exception exception) {
            batchImportFileStatusService.markError(fileUUID, null);
            taskExecutionLifecycleService.markFailed(executionUUID, null, summarizeFailure(exception));
            return false;
        }
    }

    private String summarizeFailure(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }
}
