package customer.batchimportcat.batchtask.runtime;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.stereotype.Component;

import customer.batchimportcat.batch.processors.BatchImportProcessor;
import customer.batchimportcat.service.cqn.ProcessorArtifactResolver;

@Component
public class DynamicProcessorExecutor {
    private final ProcessorArtifactResolver processorArtifactResolver;
    private final ProcessorArtifactContentLoader processorArtifactContentLoader;
    private final ProcessorClassLoaderFactory processorClassLoaderFactory;

    public DynamicProcessorExecutor(ProcessorArtifactResolver processorArtifactResolver,
            ProcessorArtifactContentLoader processorArtifactContentLoader,
            ProcessorClassLoaderFactory processorClassLoaderFactory) {
        this.processorArtifactResolver = processorArtifactResolver;
        this.processorArtifactContentLoader = processorArtifactContentLoader;
        this.processorClassLoaderFactory = processorClassLoaderFactory;
    }

    public LoadedBatchImportProcessor loadProcessor(String executionUUID, String expectedProcessKey) {
        ResolvedProcessorArtifact artifact = processorArtifactResolver.resolve(executionUUID);
        if (!expectedProcessKey.equals(artifact.processKey())) {
            throw new IllegalStateException("Execution " + executionUUID + " expects process key " + expectedProcessKey
                    + " but artifact is bound to " + artifact.processKey() + ".");
        }

        byte[] content = processorArtifactContentLoader.loadContent(artifact);
        validateChecksum(artifact.artifactChecksum(), content);

        ProcessorArtifactClassLoader classLoader = processorClassLoaderFactory.create(artifact.processorArtifactId(),
                content);
        try {
            Class<?> entryPointClass = Class.forName(artifact.entryPointClass(), true, classLoader.classLoader());
            Object instance = entryPointClass.getDeclaredConstructor().newInstance();
            if (!(instance instanceof BatchImportProcessor processor)) {
                throw new IllegalStateException("Entry point class " + artifact.entryPointClass()
                        + " does not implement BatchImportProcessor.");
            }
            if (!expectedProcessKey.equals(processor.getProcessKey())) {
                throw new IllegalStateException("Loaded processor process key " + processor.getProcessKey()
                        + " does not match expected " + expectedProcessKey + ".");
            }
            return new LoadedBatchImportProcessor(artifact, processor, classLoader);
        } catch (Exception exception) {
            classLoader.close();
            throw new IllegalStateException("Failed to load processor artifact " + artifact.processorArtifactId() + ".",
                    exception);
        }
    }

    private void validateChecksum(String expectedChecksum, byte[] content) {
        if (expectedChecksum == null || expectedChecksum.isBlank()) {
            return;
        }
        String actualChecksum = sha256(content);
        if (!expectedChecksum.equalsIgnoreCase(actualChecksum)) {
            throw new IllegalStateException("Processor artifact checksum mismatch. Expected " + expectedChecksum
                    + " but was " + actualChecksum + ".");
        }
    }

    private String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] checksum = digest.digest(content);
            StringBuilder builder = new StringBuilder(checksum.length * 2);
            for (byte value : checksum) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to calculate processor artifact checksum.", exception);
        }
    }
}
