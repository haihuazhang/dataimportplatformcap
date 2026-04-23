package customer.batchimportcat.model;

public record ProcessorArtifactBinding(
        String id,
        String processKey,
        String version,
        String description,
        String artifactChecksum,
        String entryPointClass) {
}
