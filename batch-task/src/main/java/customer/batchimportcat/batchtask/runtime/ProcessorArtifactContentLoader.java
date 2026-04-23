package customer.batchimportcat.batchtask.runtime;

import org.springframework.stereotype.Component;

import customer.batchimportcat.batch.runtime.ArtifactStorageType;

@Component
public class ProcessorArtifactContentLoader {
    public byte[] loadContent(ResolvedProcessorArtifact artifact) {
        ArtifactStorageType storageType = ArtifactStorageType.fromValue(artifact.artifactStorageType());
        return switch (storageType) {
            case DB_MEDIA -> loadDbMedia(artifact);
            case S3, OBJECT_STORAGE -> throw new IllegalStateException(
                    "Artifact storage type " + storageType + " is not implemented in v1.");
        };
    }

    private byte[] loadDbMedia(ResolvedProcessorArtifact artifact) {
        if (artifact.mediaContent() == null || artifact.mediaContent().length == 0) {
            throw new IllegalStateException("Processor artifact " + artifact.processorArtifactId()
                    + " does not contain DB media content.");
        }
        return artifact.mediaContent();
    }
}
