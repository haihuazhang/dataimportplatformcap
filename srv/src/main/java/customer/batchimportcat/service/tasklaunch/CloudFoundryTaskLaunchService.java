package customer.batchimportcat.service.tasklaunch;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import customer.batchimportcat.model.TaskLaunchProperties;
import customer.batchimportcat.model.TaskLaunchRequest;
import customer.batchimportcat.model.TaskLaunchResult;

@Service
@ConditionalOnProperty(prefix = "batchimport.task", name = "launcher-type", havingValue = "cloudfoundry")
public class CloudFoundryTaskLaunchService implements TaskLaunchService {
    private static final Pattern TASK_ID_PATTERN = Pattern.compile("task\\s+([\\w-]+)", Pattern.CASE_INSENSITIVE);

    private final TaskLaunchProperties properties;

    public CloudFoundryTaskLaunchService(TaskLaunchProperties properties) {
        this.properties = properties;
    }

    @Override
    public TaskLaunchResult launch(TaskLaunchRequest request) {
        try {
            List<String> command = new ArrayList<>();
            command.add(properties.getCloudfoundry().getCfBinary());
            command.add("run-task");
            command.add(request.taskHostApp());
            command.add("--name");
            command.add(request.taskName());
            command.add("--command");
            command.add(request.command());
            if (request.memoryMb() != null) {
                command.add("-m");
                command.add(request.memoryMb() + "M");
            }
            if (request.diskMb() != null) {
                command.add("-k");
                command.add(request.diskMb() + "M");
            }

            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return new TaskLaunchResult(false, request.launcherType(), null, request.taskName(), Instant.now(),
                        output, output);
            }
            return new TaskLaunchResult(true, request.launcherType(), parseTaskId(output), request.taskName(),
                    Instant.now(), output, null);
        } catch (Exception exception) {
            return new TaskLaunchResult(false, request.launcherType(), null, request.taskName(), Instant.now(), null,
                    exception.getMessage());
        }
    }

    private String parseTaskId(String output) {
        Matcher matcher = TASK_ID_PATTERN.matcher(output);
        return matcher.find() ? matcher.group(1) : null;
    }
}
