package customer.batchimportcat.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Component;

@Component
public class TaskLaunchRequestFactory {
    private final TaskLaunchProperties properties;

    public TaskLaunchRequestFactory(TaskLaunchProperties properties) {
        this.properties = properties;
    }

    public TaskLaunchRequest create(String executionUUID, BatchImportFileLaunchContext fileContext,
            ProcessorArtifactBinding artifact) {
        String launcherType = launcherType();
        return new TaskLaunchRequest(
                executionUUID,
                fileContext.fileUUID(),
                fileContext.processKey(),
                artifact.id(),
                artifact.version(),
                artifact.artifactChecksum(),
                properties.getHostApp(),
                buildTaskName(executionUUID),
                buildCommand(executionUUID, fileContext.fileUUID()),
                launcherType,
                properties.getMemoryMb(),
                properties.getDiskMb(),
                properties.getEnvironment(),
                properties.getTimeout());
    }

    public String taskHostApp() {
        return properties.getHostApp();
    }

    public String launcherType() {
        String launcherType = properties.getLauncherType();
        if (launcherType == null || launcherType.isBlank()) {
            return "local";
        }
        return launcherType.trim().toLowerCase();
    }

    private String buildTaskName(String executionUUID) {
        String suffix = executionUUID == null ? "unknown" : executionUUID.replace("-", "");
        if (suffix.length() > 12) {
            suffix = suffix.substring(0, 12);
        }
        return "batch-import-" + suffix;
    }

    private String buildCommand(String executionUUID, String fileUUID) {
        String launcherType = launcherType();
        if ("cloudfoundry".equals(launcherType)) {
            return interpolate(properties.getCloudfoundry().getCommandTemplate(), executionUUID, fileUUID);
        }

        String localTemplate = properties.getLocal().getCommandTemplate();
        if (localTemplate != null && !localTemplate.isBlank()) {
            return interpolate(localTemplate, executionUUID, fileUUID)
                    .replace("{repoRoot}", quote(resolveRepositoryRoot().toString()));
        }

        Path repositoryRoot = resolveRepositoryRoot();
        Path batchTaskJar = repositoryRoot.resolve(properties.getLocal().getBatchTaskJar()).normalize();
        String javaBinary = resolveJavaBinary();
        if (Files.isRegularFile(batchTaskJar)) {
            return quote(javaBinary) + " -jar " + quote(batchTaskJar.toString())
                    + " --fileUUID=" + fileUUID
                    + " --executionUUID=" + executionUUID;
        }

        Path pomFile = repositoryRoot.resolve("pom.xml");
        return "mvn -q -f " + quote(pomFile.toString())
                + " -pl batch-task -am -DskipTests spring-boot:run"
                + " -Dspring-boot.run.arguments='--fileUUID=" + fileUUID + " --executionUUID=" + executionUUID + "'";
    }

    private Path resolveRepositoryRoot() {
        Path current = Paths.get("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("pom.xml"))
                    && Files.isDirectory(current.resolve("srv"))
                    && Files.isDirectory(current.resolve("batch-task"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Failed to locate repository root for local batch task launch.");
    }

    private String resolveJavaBinary() {
        if (properties.getLocal().getJavaBinary() != null && !properties.getLocal().getJavaBinary().isBlank()) {
            return properties.getLocal().getJavaBinary();
        }
        return Paths.get(System.getProperty("java.home"), "bin", "java").toString();
    }

    private String interpolate(String template, String executionUUID, String fileUUID) {
        return template
                .replace("{executionUUID}", executionUUID)
                .replace("{fileUUID}", fileUUID);
    }

    private String quote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
