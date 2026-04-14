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

import customer.batchimportcat.batch.dynamic.DynamicDataFactory;
import customer.batchimportcat.batch.dynamic.dto.BatchImportProcessResult;
import customer.batchimportcat.batch.dynamic.dto.DynamicNode;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessContext;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessMessage;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessPayload;
import customer.batchimportcat.batch.dynamic.types.DynamicImportConfiguration;
import customer.batchimportcat.batch.dynamic.types.DynamicTableHandle;
import customer.batchimportcat.batch.dynamic.types.BatchImportOriginalDataRecord;
import customer.batchimportcat.batch.processors.BatchImportProcessor;
import customer.batchimportcat.batch.processors.BatchImportProcessorRegistry;
import customer.batchimportcat.consts.Constant;
import customer.batchimportcat.service.BatchImportPersistenceService;

public class ProcessKeyDelegatingItemWriter implements ItemWriter<DynamicNode>, StepExecutionListener {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final DynamicImportConfiguration configuration;
    private final String fileUUID;
    private final BatchImportProcessorRegistry processorRegistry;
    private final BatchImportPersistenceService batchImportPersistenceService;
    private final DynamicDataFactory dynamicDataFactory;

    private BatchImportProcessor processor;
    private BatchImportProcessContext processContext;
    private StepExecution stepExecution;
    private boolean hasErrors;

    public ProcessKeyDelegatingItemWriter(DynamicImportConfiguration configuration, String fileUUID,
            BatchImportProcessorRegistry processorRegistry, BatchImportPersistenceService batchImportPersistenceService,
            DynamicDataFactory dynamicDataFactory) {
        this.configuration = configuration;
        this.fileUUID = fileUUID;
        this.processorRegistry = processorRegistry;
        this.batchImportPersistenceService = batchImportPersistenceService;
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

    private void saveOriginalData(List<DynamicNode> items) {
        List<BatchImportOriginalDataRecord> dataEntries = new ArrayList<>(items.size());
        for (DynamicNode item : items) {
            dataEntries.add(new BatchImportOriginalDataRecord(
                    Long.valueOf(item.getLineNumber()),
                    item.getStructureName(),
                    toJson(item)));
        }
        batchImportPersistenceService.saveOriginalData(fileUUID, dataEntries);
    }

    private void saveMessages(List<BatchImportProcessMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        batchImportPersistenceService.saveMessages(fileUUID, messages);
    }

    private String toJson(DynamicNode node) {
        try {
            return OBJECT_MAPPER.writeValueAsString(node.asSerializableMap());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize dynamic node.", exception);
        }
    }

}
