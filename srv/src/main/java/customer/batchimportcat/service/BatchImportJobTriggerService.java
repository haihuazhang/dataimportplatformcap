package customer.batchimportcat.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class BatchImportJobTriggerService {
    private final JobLauncher jobLauncher;
    private final Job job;
    private final BatchImportFileStatusService batchImportFileStatusService;

    public BatchImportJobTriggerService(@Qualifier("asyncJobLauncher") JobLauncher jobLauncher,
            @Qualifier("batchImportJob") Job job,
            BatchImportFileStatusService batchImportFileStatusService) {
        this.jobLauncher = jobLauncher;
        this.job = job;
        this.batchImportFileStatusService = batchImportFileStatusService;
    }

    public void trigger(String fileUUID) {
        Map<String, JobParameter<?>> parameters = new HashMap<>();
        parameters.put("fileUUID", new JobParameter<>(fileUUID, String.class));

        try {
            batchImportFileStatusService.markQueued(fileUUID, null);
            jobLauncher.run(job, new JobParameters(parameters));
        } catch (Exception exception) {
            batchImportFileStatusService.markError(fileUUID, null);
        }
    }
}
