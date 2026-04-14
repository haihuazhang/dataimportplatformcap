package customer.batchimportcat.batch.itemwriters;

import java.util.LinkedHashMap;
import java.util.Map;

import customer.batchimportcat.batch.dynamic.DynamicDataFactory;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessContext;
import customer.batchimportcat.batch.dynamic.types.DynamicImportConfiguration;
import customer.batchimportcat.batch.dynamic.types.DynamicTableHandle;
import customer.batchimportcat.batch.processors.BatchImportProcessor;
import customer.batchimportcat.batch.processors.BatchImportProcessorRegistry;

public class ProcessKeyDelegatingStepState {
    private final DynamicImportConfiguration configuration;
    private final String fileUUID;
    private final BatchImportProcessorRegistry processorRegistry;
    private final DynamicDataFactory dynamicDataFactory;

    private BatchImportProcessor processor;
    private BatchImportProcessContext processContext;
    private boolean hasErrors;

    public ProcessKeyDelegatingStepState(DynamicImportConfiguration configuration, String fileUUID,
            BatchImportProcessorRegistry processorRegistry, DynamicDataFactory dynamicDataFactory) {
        this.configuration = configuration;
        this.fileUUID = fileUUID;
        this.processorRegistry = processorRegistry;
        this.dynamicDataFactory = dynamicDataFactory;
    }

    public void initialize() {
        processor = processorRegistry.get(configuration.processKey());
        processContext = buildProcessContext();
        hasErrors = false;
    }

    public BatchImportProcessor processor() {
        if (processor == null) {
            initialize();
        }
        return processor;
    }

    public BatchImportProcessContext processContext() {
        if (processContext == null) {
            initialize();
        }
        return processContext;
    }

    public void updateHasErrors(boolean hasErrors) {
        this.hasErrors = this.hasErrors || hasErrors;
    }

    public boolean hasErrors() {
        return hasErrors;
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
}
