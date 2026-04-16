package customer.batchimportcat.batch.itemwriters;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import customer.batchimportcat.batch.dynamic.dto.DynamicNode;
import customer.batchimportcat.batch.dynamic.dto.DynamicRow;
import customer.batchimportcat.batch.dynamic.dto.DynamicTable;
import customer.batchimportcat.batch.dynamic.types.DynamicImportConfiguration;
import customer.batchimportcat.batch.dynamic.types.DynamicStructureDefinition;
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

        BatchImportProcessPayload payload = processContext.createPayload(items);
        saveOriginalData(payload, processContext);

        BatchImportProcessResult result = processor.process(processContext, payload);
        if (result != null) {
            saveMessages(result.getMessages());
            stepState.updateHasErrors(result.hasErrors());
        }
    }

    private void saveOriginalData(BatchImportProcessPayload payload, BatchImportProcessContext processContext) {
        String primaryRootStructureUUID = resolvePrimaryRootStructureUUID(processContext.configuration());
        if (primaryRootStructureUUID == null) {
            return;
        }

        DynamicTable rootTable = payload.rootTablesByStructureUUID().get(primaryRootStructureUUID);
        if (rootTable == null || rootTable.isEmpty()) {
            return;
        }

        List<BatchImportOriginalDataRecord> dataEntries = new ArrayList<>(rootTable.size());
        for (DynamicRow rootRow : rootTable) {
            dataEntries.add(new BatchImportOriginalDataRecord(
                    Long.valueOf(rootRow.getLineNumber()),
                    rootTable.getStructureName(),
                    toJson(rootTable.getStructureName(), rootRow)));
        }
        batchImportPersistenceService.saveOriginalData(fileUUID, dataEntries);
    }

    private void saveMessages(List<BatchImportProcessMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        batchImportPersistenceService.saveMessages(fileUUID, messages);
    }

    private String toJson(String rootStructureName, DynamicRow rootRow) {
        try {
            LinkedHashMap<String, Object> root = new LinkedHashMap<>();
            root.put(rootStructureName, rootRow);
            return OBJECT_MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize dynamic row.", exception);
        }
    }

    private String resolvePrimaryRootStructureUUID(DynamicImportConfiguration configuration) {
        for (DynamicStructureDefinition rootStructure : configuration.rootStructures()) {
            return rootStructure.id();
        }
        return null;
    }

}
