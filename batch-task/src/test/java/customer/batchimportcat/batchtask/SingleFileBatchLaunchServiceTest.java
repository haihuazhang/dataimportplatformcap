package customer.batchimportcat.batchtask;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

import customer.batchimportcat.service.BatchImportFileStatusService;
import customer.batchimportcat.service.cqn.TaskExecutionPersistenceService;

class SingleFileBatchLaunchServiceTest {
    @Test
    void launchPersistsBatchFailureSummaryFromJobExecution() throws Exception {
        JobLauncher jobLauncher = mock(JobLauncher.class);
        Job job = mock(Job.class);
        TaskExecutionPersistenceService taskExecutionPersistenceService = mock(TaskExecutionPersistenceService.class);
        BatchImportFileStatusService batchImportFileStatusService = mock(BatchImportFileStatusService.class);
        SingleFileBatchLaunchService service = new SingleFileBatchLaunchService(jobLauncher, job,
                taskExecutionPersistenceService, batchImportFileStatusService);
        JobExecution jobExecution = new JobExecution(new JobInstance(2L, "dynamicBatchImportJob"), 99L,
                new JobParameters());
        jobExecution.setStatus(BatchStatus.FAILED);
        jobExecution.addFailureException(new IllegalStateException(
                "Failed to read processor artifact media content.",
                new ClassCastException("HANA Blob cannot be cast to InputStream")));
        when(jobLauncher.run(eq(job), any(JobParameters.class))).thenReturn(jobExecution);

        boolean result = service.launch("file-1", "execution-1");

        assertFalse(result);
        verify(taskExecutionPersistenceService).markRunning("execution-1");
        verify(taskExecutionPersistenceService).markFailed("execution-1", 2L,
                "Failed to read processor artifact media content. Root cause: HANA Blob cannot be cast to InputStream");
    }
}
