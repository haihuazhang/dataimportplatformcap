package customer.batchimportcat.batch.itemwriters;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.ql.Insert;
import com.sap.cds.ql.cqn.CqnInsert;
import com.sap.cds.services.cds.CqnService;

import cds.gen.dataimportservice.BatchImportData;
import cds.gen.dataimportservice.BatchImportData_;
import cds.gen.dataimportservice.BatchImportMessage;
import cds.gen.dataimportservice.BatchImportMessage_;
import customer.batchimportcat.batch.dynamic.BatchImportProcessResult;
import customer.batchimportcat.batch.dynamic.DynamicDataFactory;
import customer.batchimportcat.batch.dynamic.DynamicImportConfiguration;
import customer.batchimportcat.batch.dynamic.DynamicTableHandle;
import customer.batchimportcat.batch.dynamic.dto.DynamicNode;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessContext;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessMessage;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessPayload;
import customer.batchimportcat.batch.processors.BatchImportProcessor;
import customer.batchimportcat.batch.processors.BatchImportProcessorRegistry;
import customer.batchimportcat.consts.Constant;

public class ProcessKeyDelegatingItemWriter implements ItemWriter<DynamicNode>, StepExecutionListener {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final DynamicImportConfiguration configuration;
    private final String fileUUID;
    private final BatchImportProcessorRegistry processorRegistry;
    private final CqnService dataImportService;
    private final DynamicDataFactory dynamicDataFactory;

    private BatchImportProcessor processor;
    private BatchImportProcessContext processContext;
    private StepExecution stepExecution;
    private boolean hasErrors;

    public ProcessKeyDelegatingItemWriter(DynamicImportConfiguration configuration, String fileUUID,
            BatchImportProcessorRegistry processorRegistry, CqnService dataImportService,
            DynamicDataFactory dynamicDataFactory) {
        this.configuration = configuration;
        this.fileUUID = fileUUID;
        this.processorRegistry = processorRegistry;
        this.dataImportService = dataImportService;
        this.dynamicDataFactory = dynamicDataFactory;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
        processor = processorRegistry.get(configuration.processKey());
        processContext = buildProcessContext();
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        if (hasErrors && this.stepExecution != null) {
            this.stepExecution.getJobExecution()
                    .getExecutionContext()
                    .put(Constant.HAS_PROCESSING_ERRORS, Boolean.TRUE);
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
        BatchImportProcessPayload payload = processContext.createPayload(items);
        BatchImportProcessResult result = processor.process(processContext, payload);
        if (result != null) {
            saveMessages(result.getMessages());
            hasErrors = hasErrors || result.hasErrors();
        }
    }

    private BatchImportProcessContext buildProcessContext() {
        Map<String, DynamicTableHandle> handlesByStructureUUID = dynamicDataFactory.createHandles(configuration);
        Map<String, DynamicTableHandle> handlesByStructureName = new LinkedHashMap<>();
        for (DynamicTableHandle handle : handlesByStructureUUID.values()) {
            handlesByStructureName.put(handle.structureName(), handle);
        }
        return new BatchImportProcessContext(
                fileUUID,
                configuration,
                handlesByStructureUUID,
                Map.copyOf(handlesByStructureName),
                dynamicDataFactory);
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
}
