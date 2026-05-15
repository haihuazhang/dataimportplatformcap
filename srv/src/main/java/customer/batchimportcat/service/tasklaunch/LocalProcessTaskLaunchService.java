package customer.batchimportcat.service.tasklaunch;

import java.time.Instant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import customer.batchimportcat.model.TaskLaunchProperties;
import customer.batchimportcat.model.TaskLaunchRequest;
import customer.batchimportcat.model.TaskLaunchResult;

@Service
@ConditionalOnProperty(prefix = "batchimport.task", name = "launcher-type", havingValue = "local",
        matchIfMissing = true)
public class LocalProcessTaskLaunchService implements TaskLaunchService {
    private final TaskLaunchProperties properties;

    public LocalProcessTaskLaunchService(TaskLaunchProperties properties) {
        this.properties = properties;
    }

    @Override
    public TaskLaunchResult launch(TaskLaunchRequest request) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    properties.getLocal().getShell(),
                    "-lc",
                    request.command());
            processBuilder.redirectErrorStream(true);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            processBuilder.environment().putAll(request.environment());
            Process process = processBuilder.start();
            return new TaskLaunchResult(
                    true,
                    request.launcherType(),
                    String.valueOf(process.pid()),
                    request.taskName(),
                    request.command(),
                    Instant.now(),
                    "SUBMITTED",
                    null);
        } catch (Exception exception) {
            return new TaskLaunchResult(
                    false,
                    request.launcherType(),
                    null,
                    request.taskName(),
                    request.command(),
                    Instant.now(),
                    null,
                    exception.getMessage());
        }
    }
}
