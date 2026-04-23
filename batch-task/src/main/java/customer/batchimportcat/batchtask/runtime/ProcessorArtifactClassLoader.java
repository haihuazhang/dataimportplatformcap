package customer.batchimportcat.batchtask.runtime;

import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

public record ProcessorArtifactClassLoader(Path tempJarPath, URLClassLoader classLoader) implements AutoCloseable {
    @Override
    public void close() {
        try {
            classLoader.close();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to close processor class loader.", exception);
        } finally {
            try {
                Files.deleteIfExists(tempJarPath);
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to delete temp processor jar " + tempJarPath + ".", exception);
            }
        }
    }
}
