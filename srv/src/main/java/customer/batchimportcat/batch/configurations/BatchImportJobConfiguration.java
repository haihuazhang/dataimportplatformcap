package customer.batchimportcat.batch.configurations;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import com.sap.cds.services.cds.CqnService;

import cds.gen.dataimportservice.DataImportService_;
import customer.batchimportcat.batch.dynamic.BatchImportProcessorRegistry;
import customer.batchimportcat.batch.dynamic.DynamicHierarchyItemReader;
import customer.batchimportcat.batch.dynamic.DynamicImportConfiguration;
import customer.batchimportcat.batch.dynamic.DynamicNode;
import customer.batchimportcat.batch.dynamic.ProcessKeyDelegatingItemWriter;
import customer.batchimportcat.batch.jobLaunchers.AsyncTransactionalJobLauncher;
import customer.batchimportcat.batch.tasklets.GetBatchImportConfigTasklet;

@Configuration
@EnableBatchProcessing(dataSourceRef = "ds-db", transactionManagerRef = "tx-db")
public class BatchImportJobConfiguration {
    @Bean
    public Job batchImportJob(JobRepository jobRepository,
            @Qualifier("getBatchImportConfigStep") Step getBatchImportConfigStep,
            @Qualifier("processingDynamicData") Step processingDynamicData) {
        return new JobBuilder("dynamicBatchImportJob", jobRepository)
                .start(getBatchImportConfigStep)
                .next(processingDynamicData)
                .build();
    }

    @Bean
    public Step getBatchImportConfigStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            @Qualifier("getBatchImportConfigTasklet") Tasklet getBatchImportConfigTasklet) {
        return new StepBuilder("getBatchImportConfigStep", jobRepository)
                .tasklet(getBatchImportConfigTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet getBatchImportConfigTasklet() {
        return new GetBatchImportConfigTasklet();
    }

    @Bean
    @StepScope
    public DynamicHierarchyItemReader dynamicHierarchyItemReader(
            @Value("#{jobExecutionContext['fileContent']}") byte[] fileContent,
            @Value("#{jobExecutionContext['dynamicConfig']}") DynamicImportConfiguration dynamicConfig) {
        return new DynamicHierarchyItemReader(fileContent, dynamicConfig);
    }

    @Bean
    @StepScope
    public ProcessKeyDelegatingItemWriter processKeyDelegatingItemWriter(
            @Value("#{jobExecutionContext['dynamicConfig']}") DynamicImportConfiguration dynamicConfig,
            @Value("#{jobParameters['fileUUID']}") String fileUUID,
            BatchImportProcessorRegistry processorRegistry,
            @Qualifier(DataImportService_.CDS_NAME) CqnService dataImportService) {
        return new ProcessKeyDelegatingItemWriter(dynamicConfig, fileUUID, processorRegistry, dataImportService);
    }

    @Bean
    public Step processingDynamicData(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            DynamicHierarchyItemReader dynamicHierarchyItemReader,
            ProcessKeyDelegatingItemWriter processKeyDelegatingItemWriter) {
        return new StepBuilder("processingDynamicData", jobRepository)
                .<DynamicNode, DynamicNode>chunk(50, transactionManager)
                .reader(dynamicHierarchyItemReader)
                .writer(processKeyDelegatingItemWriter)
                .listener(processKeyDelegatingItemWriter)
                .build();
    }

    @Bean
    public JobLauncher asyncJobLauncher(JobRepository jobRepository) {
        AsyncTransactionalJobLauncher jobLauncher = new AsyncTransactionalJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor());
        try {
            jobLauncher.afterPropertiesSet();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to initialize async job launcher.", exception);
        }
        return jobLauncher;
    }
}
