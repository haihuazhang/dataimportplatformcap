package customer.batchimportcat.batch.tasklets;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.sap.cds.Result;
import com.sap.cds.Row;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.CqnValidationException;
import com.sap.cds.services.cds.CqnService;

import cds.gen.dataimportservice.BatchImportConfig_;
import cds.gen.dataimportservice.BatchImportFile_;
import cds.gen.dataimportservice.DataImportService_;
import customer.batchimportcat.batch.exceptions.BatchExceptionsUtil;
import customer.batchimportcat.batch.exceptions.BatchFileNotFound;

public class GetBatchImportConfigTasklet implements Tasklet {

    @Autowired
    @Qualifier(DataImportService_.CDS_NAME)
    CqnService dataimportService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // System.out.println(
        // "Hello World"
        // );
        String fileUUID = chunkContext.getStepContext().getJobParameters().get("fileUUID").toString();

        // get file content from DB
        CqnSelect select = Select.from(BatchImportFile_.class).where(file -> file.get("ID").eq(fileUUID));
        // byId(fileUUID);
        try {
            Result result = dataimportService.run(select);
            if (result.rowCount() > 0) {
                // put configuration and file content to the job execution context
                InputStream inputStream = (InputStream) result.list().get(0).get("Attachment");
                byte[] byteStream = inputStream.readAllBytes();
                
                // chunkContext.getStepContext().getJobExecutionContext().put("fileContent",
                // inputStream);
                contribution.getStepExecution().getJobExecution().getExecutionContext().put("fileContent", byteStream);

                // get configuration uuid
                String configUUID = result.list().get(0).get("ConfigUUID").toString();

                CqnSelect selectConfig = Select.from(BatchImportConfig_.class)
                        .where(config -> config.get("ID").eq(configUUID));
                // .byId(configUUID);
                Result resultConfig = dataimportService.run(selectConfig);
                // set configuration
                if (resultConfig.rowCount() > 0) {
                    Row configRow = resultConfig.list().get(0);
                    Map<String, Serializable> serializableConfigData = configRow.entrySet().stream()
                            .filter(entry -> entry.getValue() instanceof Serializable)
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> (Serializable) e.getValue()));

                    contribution.getStepExecution().getJobExecution().getExecutionContext()
                            // chunkContext.getStepContext().getJobExecutionContext().put("configData",
                            .put("configData",
                                    serializableConfigData);

                } else {
                    // exception of batch config not found
                    throw BatchExceptionsUtil.geBatchConfigNotFound(configUUID);
                }

            } else {
                // exception of batch file record not found
                throw BatchExceptionsUtil.getBatchFileNotFound(fileUUID);
            }
        } catch (CqnValidationException e) {
            // TODO: handle exception
            throw new BatchFileNotFound(e.getMessage());
        }

        return RepeatStatus.FINISHED;
    }
}
