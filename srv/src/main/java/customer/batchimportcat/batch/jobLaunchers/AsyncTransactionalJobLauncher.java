package customer.batchimportcat.batch.jobLaunchers;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

public class AsyncTransactionalJobLauncher extends TaskExecutorJobLauncher {
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public JobExecution run(Job job, JobParameters jobParameters) throws JobExecutionAlreadyRunningException,
            JobRestartException, JobInstanceAlreadyCompleteException, JobParametersInvalidException {
        return super.run(job, jobParameters);
    }
}
