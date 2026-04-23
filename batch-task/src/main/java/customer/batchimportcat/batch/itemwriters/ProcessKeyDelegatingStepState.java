package customer.batchimportcat.batch.itemwriters;

import java.util.LinkedHashMap;
import java.util.Map;

import customer.batchimportcat.batch.dynamic.DynamicDataFactory;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessContext;
import customer.batchimportcat.batch.dynamic.types.DynamicImportConfiguration;
import customer.batchimportcat.batch.dynamic.types.DynamicTableHandle;
import customer.batchimportcat.batch.processors.BatchImportProcessor;
import customer.batchimportcat.batch.runtime.ProcessorRuntime;
import customer.batchimportcat.batchtask.runtime.DynamicProcessorExecutor;
import customer.batchimportcat.batchtask.runtime.LoadedBatchImportProcessor;

public class ProcessKeyDelegatingStepState implements AutoCloseable {
    private final DynamicImportConfiguration configuration;
    private final String fileUUID;
    private final String executionUUID;
    private final DynamicDataFactory dynamicDataFactory;
    private final DynamicProcessorExecutor dynamicProcessorExecutor;
    private final ProcessorRuntime processorRuntime;

    private LoadedBatchImportProcessor loadedProcessor;
    private BatchImportProcessContext processContext;
    private boolean hasErrors;

    public ProcessKeyDelegatingStepState(DynamicImportConfiguration configuration, String fileUUID, String executionUUID,
            DynamicDataFactory dynamicDataFactory, DynamicProcessorExecutor dynamicProcessorExecutor,
            ProcessorRuntime processorRuntime) {
        this.configuration = configuration;
        this.fileUUID = fileUUID;
        this.executionUUID = executionUUID;
        this.dynamicDataFactory = dynamicDataFactory;
        this.dynamicProcessorExecutor = dynamicProcessorExecutor;
        this.processorRuntime = processorRuntime;
    }

    public void initialize() {
        loadedProcessor = dynamicProcessorExecutor.loadProcessor(executionUUID, configuration.processKey());
        processContext = buildProcessContext();
        hasErrors = false;
    }

    public BatchImportProcessor processor() {
        if (loadedProcessor == null) {
            initialize();
        }
        return loadedProcessor.processor();
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

    @Override
    public void close() {
        if (loadedProcessor != null) {
            loadedProcessor.close();
            loadedProcessor = null;
        }
        processContext = null;
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
                processorRuntime);
    }
}
