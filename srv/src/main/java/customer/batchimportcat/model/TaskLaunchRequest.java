package customer.batchimportcat.model;

import java.time.Duration;
import java.util.Map;

public record TaskLaunchRequest(
        String executionUUID,
        String fileUUID,
        String processKey,
        String processorArtifactId,
        String processorVersion,
        String artifactChecksum,
        String taskHostApp,
        String taskName,
        String command,
        String launcherType,
        Integer memoryMb,
        Integer diskMb,
        Map<String, String> environment,
        Duration timeout) {
}
