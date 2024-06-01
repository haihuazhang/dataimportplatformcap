package customer.batchimportcat.batch.configurations;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import customer.batchimportcat.batch.tasklets.SimpleTasklet;

@Configuration
@EnableBatchProcessing(dataSourceRef = "ds-db", transactionManagerRef = "tx-db")
public class SimpleJobConfiguration {
    @Bean
    public Job quartBatchJob(JobRepository jobRepository,
            @Qualifier("getSimpleStep") Step getSimpleStep) {
        return new JobBuilder("quartBatchJob", jobRepository)
                .start(getSimpleStep)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet getSimpleTeTasklet() {
        return new SimpleTasklet();
    }

    @Bean
    public Step getSimpleStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            @Qualifier("getSimpleTeTasklet") Tasklet getSimpleTeTasklet) {
        return new StepBuilder("getSimpleTeTasklet", jobRepository)
                // .tasklet(new HelloWorldTasklet(), transactionManager)
                .tasklet(getSimpleTeTasklet, transactionManager)
                .build();
    }
}
