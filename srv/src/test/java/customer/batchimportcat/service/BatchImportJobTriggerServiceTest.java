package customer.batchimportcat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

class BatchImportJobTriggerServiceTest {
    @Test
    void triggerLaunchesJobAfterQueued() {
        List<String> calls = new ArrayList<>();
        RecordingJobLauncher jobLauncher = new RecordingJobLauncher(calls);
        Job job = createJob();
        RecordingStatusService statusService = new RecordingStatusService(calls);

        BatchImportJobTriggerService service = new BatchImportJobTriggerService(jobLauncher, job, statusService);

        service.trigger("file-1");

        assertEquals(List.of("queued:file-1:null", "run:file-1"), calls);
        assertSame(job, jobLauncher.job);
        assertEquals("file-1", jobLauncher.jobParameters.getString("fileUUID"));
    }

    @Test
    void triggerMarksErrorWhenLaunchFails() {
        List<String> calls = new ArrayList<>();
        RecordingJobLauncher jobLauncher = new RecordingJobLauncher(calls);
        jobLauncher.exception = new IllegalStateException("boom");
        Job job = createJob();
        RecordingStatusService statusService = new RecordingStatusService(calls);

        BatchImportJobTriggerService service = new BatchImportJobTriggerService(jobLauncher, job, statusService);

        service.trigger("file-3");

        assertEquals(List.of("queued:file-3:null", "run:file-3", "error:file-3:null"), calls);
    }

    private static Job createJob() {
        return new Job() {
            @Override
            public String getName() {
                return "testJob";
            }

            @Override
            public void execute(JobExecution execution) {
            }
        };
    }

    private static final class RecordingJobLauncher implements JobLauncher {
        private final List<String> calls;
        private RuntimeException exception;
        private Job job;
        private JobParameters jobParameters;

        private RecordingJobLauncher(List<String> calls) {
            this.calls = calls;
        }

        @Override
        public JobExecution run(Job job, JobParameters jobParameters) {
            this.job = job;
            this.jobParameters = jobParameters;
            calls.add("run:" + jobParameters.getString("fileUUID"));
            if (exception != null) {
                throw exception;
            }
            return new JobExecution(123L, jobParameters);
        }
    }

    private static final class RecordingStatusService extends BatchImportFileStatusService {
        private final List<String> calls;

        private RecordingStatusService(List<String> calls) {
            super(null);
            this.calls = calls;
        }

        @Override
        public void markQueued(String fileUUID, Long jobInstanceId) {
            calls.add("queued:" + fileUUID + ":" + jobInstanceId);
        }

        @Override
        public void markError(String fileUUID, Long jobInstanceId) {
            calls.add("error:" + fileUUID + ":" + jobInstanceId);
        }
    }
}
