package customer.batchimportcat.quartz.jobs;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.quartz.QuartzJobBean;

public class TestJob extends QuartzJobBean {

    JobLauncher jobLauncher;

    Job job;

    public TestJob(@Qualifier("asyncJobLauncher") JobLauncher jobLauncher, @Qualifier("quartBatchJob") Job job) {
        this.jobLauncher = jobLauncher;
        this.job = job;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        System.out.println("executing quartz job");
        System.out.println(context.getFireTime());
        try {

            Map<String, JobParameter<?>> parMap = new HashMap<>();

            parMap.put("fileUUID", new JobParameter<>(UUID.randomUUID().toString(), String.class));

            JobExecution jobExecution = jobLauncher.run(job, new JobParameters(parMap));
        } catch (Exception e) {
            // TODO: handle exception
            System.out.println(e.getMessage());
        }
    }

}
