package customer.batchimportcat.batchtask.runtime;

import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.springframework.stereotype.Component;

import customer.batchimportcat.batch.processors.BatchImportProcessor;

@Component
public class ProcessorClassLoaderFactory {
    public ProcessorArtifactClassLoader create(String artifactId, byte[] content) {
        try {
            Path tempJarPath = Files.createTempFile("processor-artifact-" + artifactId + "-", ".jar");
            Files.write(tempJarPath, content, StandardOpenOption.TRUNCATE_EXISTING);
            URLClassLoader classLoader = new URLClassLoader(new java.net.URL[] { tempJarPath.toUri().toURL() },
                    BatchImportProcessor.class.getClassLoader());
            return new ProcessorArtifactClassLoader(tempJarPath, classLoader);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to create processor class loader for artifact " + artifactId + ".",
                    exception);
        }
    }
}
