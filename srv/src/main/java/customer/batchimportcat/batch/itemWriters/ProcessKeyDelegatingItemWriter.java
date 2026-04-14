package customer.batchimportcat.batch.itemwriters;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import customer.batchimportcat.batch.dynamic.dto.DynamicNode;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessContext;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessMessage;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessPayload;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessResult;
import customer.batchimportcat.batch.dynamic.types.BatchImportOriginalDataRecord;
import customer.batchimportcat.batch.processors.BatchImportProcessor;
import customer.batchimportcat.service.BatchImportPersistenceService;

public class ProcessKeyDelegatingItemWriter implements ItemWriter<DynamicNode> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String fileUUID;
    private final BatchImportPersistenceService batchImportPersistenceService;
    private final ProcessKeyDelegatingStepState stepState;

    public ProcessKeyDelegatingItemWriter(String fileUUID,
            BatchImportPersistenceService batchImportPersistenceService,
            ProcessKeyDelegatingStepState stepState) {
        this.fileUUID = fileUUID;
        this.batchImportPersistenceService = batchImportPersistenceService;
        this.stepState = stepState;
    }

    @Override
    public void write(Chunk<? extends DynamicNode> chunk) throws Exception {
        BatchImportProcessor processor = stepState.processor();
        BatchImportProcessContext processContext = stepState.processContext();
        List<DynamicNode> items = new ArrayList<>(chunk.getItems());
        if (items.isEmpty()) {
            return;
        }

        saveOriginalData(items);
        BatchImportProcessPayload payload = processContext.createPayload(items);
        BatchImportProcessResult result = processor.process(processContext, payload);
        if (result != null) {
            saveMessages(result.getMessages());
            stepState.updateHasErrors(result.hasErrors());
        }
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
