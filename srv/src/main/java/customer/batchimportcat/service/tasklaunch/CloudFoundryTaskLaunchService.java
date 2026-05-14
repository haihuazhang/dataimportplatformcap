package customer.batchimportcat.service.tasklaunch;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.RunApplicationTaskRequest;
import org.cloudfoundry.operations.applications.Task;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import customer.batchimportcat.model.TaskLaunchProperties;
import customer.batchimportcat.model.TaskLaunchRequest;
import customer.batchimportcat.model.TaskLaunchResult;

@Service
@ConditionalOnProperty(prefix = "batchimport.task", name = "launcher-type", havingValue = "cloudfoundry")
public class CloudFoundryTaskLaunchService implements TaskLaunchService {
    private final TaskLaunchProperties properties;
    private final Function<TaskLaunchProperties.CloudFoundry, CloudFoundryOperations> operationsFactory;

    public CloudFoundryTaskLaunchService(TaskLaunchProperties properties) {
        this(properties, CloudFoundryTaskLaunchService::createCloudFoundryOperations);
    }

    CloudFoundryTaskLaunchService(TaskLaunchProperties properties,
            Function<TaskLaunchProperties.CloudFoundry, CloudFoundryOperations> operationsFactory) {
        this.properties = properties;
        this.operationsFactory = operationsFactory;
    }

    @Override
    public TaskLaunchResult launch(TaskLaunchRequest request) {
        try {
            TaskLaunchProperties.CloudFoundry cloudFoundry = properties.getCloudfoundry();
            validateCloudFoundryConfiguration(cloudFoundry);
            Task task = operationsFactory.apply(cloudFoundry)
                    .applications()
                    .runTask(buildRunTaskRequest(request))
                    .block(launchTimeout(request));
            if (task == null) {
                return new TaskLaunchResult(false, request.launcherType(), null, request.taskName(), Instant.now(),
                        null, "Cloud Foundry task creation returned no task.");
            }
            return new TaskLaunchResult(true, request.launcherType(), platformTaskId(task), task.getName(),
                    Instant.now(), task.getState() == null ? null : task.getState().getValue(), null);
        } catch (Exception exception) {
            return new TaskLaunchResult(false, request.launcherType(), null, request.taskName(), Instant.now(), null,
                    summarizeFailure(exception));
        }
    }

    private static CloudFoundryOperations createCloudFoundryOperations(TaskLaunchProperties.CloudFoundry cloudFoundry) {
        DefaultConnectionContext connectionContext = DefaultConnectionContext.builder()
                .apiHost(normalizeApiHost(cloudFoundry.getApiHost()))
                .skipSslValidation(cloudFoundry.isSkipSslValidation())
                .build();
        PasswordGrantTokenProvider tokenProvider = PasswordGrantTokenProvider.builder()
                .username(cloudFoundry.getUsername())
                .password(cloudFoundry.getPassword())
                .build();
        CloudFoundryClient cloudFoundryClient = ReactorCloudFoundryClient.builder()
                .connectionContext(connectionContext)
                .tokenProvider(tokenProvider)
                .build();
        return DefaultCloudFoundryOperations.builder()
                .cloudFoundryClient(cloudFoundryClient)
                .organization(cloudFoundry.getOrganization())
                .space(cloudFoundry.getSpace())
                .build();
    }

    private RunApplicationTaskRequest buildRunTaskRequest(TaskLaunchRequest request) {
        return RunApplicationTaskRequest.builder()
                .applicationName(request.taskHostApp())
                .command(request.command())
                .taskName(request.taskName())
                .memory(request.memoryMb())
                .disk(request.diskMb())
                .build();
    }

    private Duration launchTimeout(TaskLaunchRequest request) {
        return request.timeout() == null ? properties.getTimeout() : request.timeout();
    }

    private static void validateCloudFoundryConfiguration(TaskLaunchProperties.CloudFoundry cloudFoundry) {
        requireNonBlank(cloudFoundry.getApiHost(), "batchimport.task.cloudfoundry.api-host");
        requireNonBlank(cloudFoundry.getOrganization(), "batchimport.task.cloudfoundry.organization");
        requireNonBlank(cloudFoundry.getSpace(), "batchimport.task.cloudfoundry.space");
        requireNonBlank(cloudFoundry.getUsername(), "batchimport.task.cloudfoundry.username");
        requireNonBlank(cloudFoundry.getPassword(), "batchimport.task.cloudfoundry.password");
    }

    private static void requireNonBlank(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " is required for Cloud Foundry task launch.");
        }
    }

    private static String normalizeApiHost(String apiHost) {
        String trimmed = apiHost.trim();
        if (!trimmed.contains("://")) {
            return trimmed;
        }
        URI uri = URI.create(trimmed);
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalStateException("batchimport.task.cloudfoundry.api-host must contain a valid host.");
        }
        return uri.getHost();
    }

    private static String platformTaskId(Task task) {
        return task.getSequenceId() == null ? null : String.valueOf(task.getSequenceId());
    }

    private static String summarizeFailure(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }
}
