package customer.batchimportcat.batchtask.runtime;

import customer.batchimportcat.batch.processors.BatchImportProcessor;

public record LoadedBatchImportProcessor(
        ResolvedProcessorArtifact artifact,
        BatchImportProcessor processor,
        ProcessorArtifactClassLoader classLoader) implements AutoCloseable {
    @Override
    public void close() {
        classLoader.close();
    }
}
