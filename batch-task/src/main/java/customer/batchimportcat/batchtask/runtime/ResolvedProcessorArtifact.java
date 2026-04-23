package customer.batchimportcat.batchtask.runtime;

public record ResolvedProcessorArtifact(
        String executionUUID,
        String processorArtifactId,
        String processKey,
        String processorVersion,
        String artifactChecksum,
        String artifactStorageType,
        String entryPointClass,
        String mediaUrl,
        byte[] mediaContent) {
}
