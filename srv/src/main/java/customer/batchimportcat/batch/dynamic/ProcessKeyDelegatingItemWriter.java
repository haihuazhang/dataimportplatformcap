package customer.batchimportcat.batch.dynamic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.Result;
import com.sap.cds.ql.Insert;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.CqnInsert;
import com.sap.cds.ql.cqn.CqnUpdate;
import com.sap.cds.services.cds.CqnService;

import cds.gen.dataimportservice.BatchImportData;
import cds.gen.dataimportservice.BatchImportData_;
import cds.gen.dataimportservice.BatchImportFile_;
import cds.gen.dataimportservice.BatchImportMessage;
import cds.gen.dataimportservice.BatchImportMessage_;
import customer.batchimportcat.batch.exceptions.BatchExceptionsUtil;

public class ProcessKeyDelegatingItemWriter implements ItemWriter<DynamicNode>, StepExecutionListener {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final DynamicImportConfiguration configuration;
    private final String fileUUID;
    private final BatchImportProcessorRegistry processorRegistry;
    private final CqnService dataImportService;

    private BatchImportProcessor processor;
    private boolean hasErrors;

    public ProcessKeyDelegatingItemWriter(DynamicImportConfiguration configuration, String fileUUID,
            BatchImportProcessorRegistry processorRegistry, CqnService dataImportService) {
        this.configuration = configuration;
        this.fileUUID = fileUUID;
        this.processorRegistry = processorRegistry;
        this.dataImportService = dataImportService;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        processor = processorRegistry.getRequired(configuration.processKey());
        updateStatus("R", "Running", 2);
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        if (stepExecution.getStatus().isUnsuccessful() || hasErrors) {
            updateStatus("E", "Error", 1);
        } else {
            updateStatus("S", "Success", 3);
        }
        return stepExecution.getExitStatus();
    }

    @Override
    public void write(Chunk<? extends DynamicNode> chunk) throws Exception {
        List<DynamicNode> items = new ArrayList<>(chunk.getItems());
        if (items.isEmpty()) {
            return;
        }

        saveOriginalData(items);
        BatchImportProcessResult result = processor.process(new BatchImportProcessContext(fileUUID, configuration), items);
        if (result != null) {
            saveMessages(result.getMessages());
            hasErrors = hasErrors || result.hasErrors();
        }
    }

    private void saveOriginalData(List<DynamicNode> items) throws JsonProcessingException {
        List<BatchImportData> dataEntries = new ArrayList<>();
        for (DynamicNode item : items) {
            BatchImportData data = BatchImportData.create();
            data.setFileUUID(fileUUID);
            data.setLine((long) item.getLineNumber());
            data.setStructureName(item.getStructureName());
            data.setDataJson(OBJECT_MAPPER.writeValueAsString(item.asSerializableMap()));
            dataEntries.add(data);
        }
        if (!dataEntries.isEmpty()) {
            CqnInsert insert = Insert.into(BatchImportData_.class).entries(dataEntries);
            dataImportService.run(insert);
        }
    }

    private void saveMessages(List<BatchImportProcessMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        List<BatchImportMessage> messageEntries = new ArrayList<>();
        for (BatchImportProcessMessage message : messages) {
            BatchImportMessage entry = BatchImportMessage.create();
            entry.setFileUUID(fileUUID);
            entry.setLine(message.line());
            entry.setType(message.type());
            entry.setCode(message.code());
            entry.setMessage(message.message());
            entry.setDetails(message.details());
            messageEntries.add(entry);
        }
        CqnInsert insert = Insert.into(BatchImportMessage_.class).entries(messageEntries);
        dataImportService.run(insert);
    }

    private void updateStatus(String status, String statusText, int criticality) {
        Map<String, Object> data = new HashMap<>();
        data.put("Status", status);
        data.put("StatusText", statusText);
        data.put("StatusCriticality", criticality);
        CqnUpdate update = Update.entity(BatchImportFile_.class)
                .data(data)
                .where(file -> file.ID().eq(fileUUID));
        Result result = dataImportService.run(update);
        if (result.rowCount() < 0) {
            throw BatchExceptionsUtil.getBatchRecordNotSucess(BatchImportFile_.CDS_NAME, 0);
        }
    }
}
