package customer.batchimportcat.batch.configurations;

import java.io.ByteArrayInputStream;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.extensions.excel.streaming.StreamingXlsxItemReader;
import org.springframework.batch.extensions.excel.support.rowset.DefaultRowSetFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ClassifierCompositeItemWriter;
import org.springframework.batch.item.support.builder.ClassifierCompositeItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import com.sap.cds.reflect.CdsModel;
import customer.batchimportcat.batch.classifiers.CdsBatchImportClassifier;
import customer.batchimportcat.batch.exceptions.BatchExceptionsUtil;
import customer.batchimportcat.batch.itemReaders.columnNameExtrators.CdsColumnNameExtrators;
import customer.batchimportcat.batch.itemReaders.rowMappers.CdsWapperRowMapper;
import customer.batchimportcat.batch.jobLaunchers.AsyncTransactionalJobLauncher;
import customer.batchimportcat.batch.tasklets.GetBatchImportConfigTasklet;

// import javax.sql.DataSource;

@Configuration
@EnableBatchProcessing(dataSourceRef = "ds-db", transactionManagerRef = "tx-db")
// @Import(Datasource)
public class BatchImportJobConfiguration {
    // @Autowired
    // @Qualifier("ds-db")
    // DataSource dataSource;
    // @Bean
    // (name = "batchTransactionManager")
    // public JdbcTransactionManager transactionManager(DataSource dataSource) {
    // return new JdbcTransactionManager(dataSource);
    // }

    @Autowired
    CdsModel cdsModel;

    @Autowired
    DefaultListableBeanFactory defaultListableBeanFactory;

    @Bean
    public Job batchImportJob(JobRepository jobRepository,
            @Qualifier("getBatchImportConfigStep") Step getBatchImportConfigStep,
            @Qualifier("processingExcelData") Step processingExcelData) {
        return new JobBuilder("helloWorldJob", jobRepository)
                .start(getBatchImportConfigStep)
                .next(processingExcelData)
                .build();
    }

    @Bean
    public Step getBatchImportConfigStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            @Qualifier("getBatchImportConfigTasklet") Tasklet getBatchImportConfigTasklet) {
        return new StepBuilder("getBatchImportConfigStep", jobRepository)
                // .tasklet(new HelloWorldTasklet(), transactionManager)
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
    public StreamingXlsxItemReader xlsxItemReader(
            // RowMapper rowMapper,
            @Value("#{jobExecutionContext['fileContent']}") byte[] fileContent,
            @Value("#{jobExecutionContext['configData']}") Map<String, ? extends Object> configData) {
        // get the Input stream
        StreamingXlsxItemReader reader = new StreamingXlsxItemReader();

        ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContent);
        reader.setResource(new InputStreamResource(inputStream));
        // set row mapper
        // BeanWrapperRowMapper<? extends Object> rowMapper = new
        // BeanWrapperRowMapper<>();
        CdsWapperRowMapper<Class<?>> rowMapper = new CdsWapperRowMapper<>();

        try {
            Class structClass;
            if (!configData.containsKey("StructName")) {
                throw BatchExceptionsUtil.getBatchConfigNotFoundBecauseNoStruct(configData.get("ID").toString());
            }

            // get structure name
            String structureName = configData.get("StructName").toString();

            // capitalize 大写
            String capitalizeStructureName = StringUtils.capitalize(structureName);

            // add package name
            String structureFullName = new StringBuilder("cds.gen.").append(capitalizeStructureName).toString();

            // get structure class
            structClass = Class.forName(structureFullName);

            rowMapper.setTargetType(
                    structClass);

            reader.setRowMapper(rowMapper);

            // set RowSetFactory and ColumnNameExtrator
            DefaultRowSetFactory rowSetFactory = new DefaultRowSetFactory();
            CdsColumnNameExtrators cdsColumnNameExtractors = new CdsColumnNameExtrators(structureName, cdsModel);
            cdsColumnNameExtractors.getColumnNameByCDS();
            rowSetFactory.setColumnNameExtractor(cdsColumnNameExtractors);
            reader.setRowSetFactory(rowSetFactory);

            // get start from line
            int startLine;
            try {
                startLine = (Integer) configData.get("StartLine");
            } catch (Exception e) {
                // TODO: handle exception
                startLine = 1;
            }

            reader.setLinesToSkip(
                    startLine);

        } catch (ClassNotFoundException e) {

            throw BatchExceptionsUtil.getBatchConfigNotFoundBecauseNoStruct(e);
        }
        return reader;
    }

    @Bean
    @StepScope
    public ClassifierCompositeItemWriter<Object> classifierWriter(
            @Value("#{jobExecutionContext['configData']}") Map<String, ? extends Object> configData) {

        // return new SimpleItemWriter<>();

        ClassifierCompositeItemWriterBuilder<Object> classifierCompositeItemWriterBuilder = new ClassifierCompositeItemWriterBuilder<>();
        classifierCompositeItemWriterBuilder
                .classifier(new CdsBatchImportClassifier<Object>(defaultListableBeanFactory, cdsModel, configData));
        return classifierCompositeItemWriterBuilder.build();
    }

    @Bean
    public Step processingExcelData(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            ItemReader xlsxItemReader, ItemWriter classifierWriter) {

        return new StepBuilder("processingExcelData", jobRepository)
                .chunk(100, transactionManager)
                .reader(xlsxItemReader)
                .writer(classifierWriter)
                .build();
    }

    @Bean
    public JobLauncher asyncJobLauncher(JobRepository jobRepository) {
        AsyncTransactionalJobLauncher jobLauncher = new AsyncTransactionalJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor());
        try {
            jobLauncher.afterPropertiesSet();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return jobLauncher;
    }

}
