package customer.batchimportcat.service.tasklaunch;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v3.applications.ApplicationResource;
import org.cloudfoundry.client.v3.applications.GetApplicationCurrentDropletRequest;
import org.cloudfoundry.client.v3.applications.ListApplicationsRequest;
import org.cloudfoundry.client.v3.organizations.ListOrganizationsRequest;
import org.cloudfoundry.client.v3.organizations.OrganizationResource;
import org.cloudfoundry.client.v3.spaces.ListSpacesRequest;
import org.cloudfoundry.client.v3.spaces.SpaceResource;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.RunApplicationTaskRequest;
import org.cloudfoundry.operations.applications.Task;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import customer.batchimportcat.model.TaskLaunchProperties;
import customer.batchimportcat.model.TaskLaunchRequest;
import customer.batchimportcat.model.TaskLaunchResult;
import reactor.core.publisher.Mono;

@Service
@ConditionalOnProperty(prefix = "batchimport.task", name = "launcher-type", havingValue = "cloudfoundry")
public class CloudFoundryTaskLaunchService implements TaskLaunchService {
    private static final String DEFAULT_COMMAND_PLACEHOLDER = "{defaultCommand}";
    private static final String WEB_PROCESS_TYPE = "web";

    private final TaskLaunchProperties properties;
    private final Function<TaskLaunchProperties.CloudFoundry, CloudFoundryConnection> connectionFactory;
    private final DefaultCommandResolver defaultCommandResolver;

    @Autowired
    public CloudFoundryTaskLaunchService(TaskLaunchProperties properties) {
        this(properties, CloudFoundryTaskLaunchService::createCloudFoundryConnection);
    }

    CloudFoundryTaskLaunchService(TaskLaunchProperties properties,
            Function<TaskLaunchProperties.CloudFoundry, CloudFoundryConnection> connectionFactory) {
        this(properties, connectionFactory, CloudFoundryTaskLaunchService::loadDefaultStartCommand);
    }

    CloudFoundryTaskLaunchService(TaskLaunchProperties properties,
            Function<TaskLaunchProperties.CloudFoundry, CloudFoundryConnection> connectionFactory,
            DefaultCommandResolver defaultCommandResolver) {
        this.properties = properties;
        this.connectionFactory = connectionFactory;
        this.defaultCommandResolver = defaultCommandResolver;
    }

    @Override
    public TaskLaunchResult launch(TaskLaunchRequest request) {
        try {
            TaskLaunchProperties.CloudFoundry cloudFoundry = properties.getCloudfoundry();
            validateCloudFoundryConfiguration(cloudFoundry);
            CloudFoundryConnection connection = connectionFactory.apply(cloudFoundry);
            String command = resolveCommand(connection, cloudFoundry, request);
            Task task = connection.operations()
                    .applications()
                    .runTask(buildRunTaskRequest(request, command))
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

    private static CloudFoundryConnection createCloudFoundryConnection(TaskLaunchProperties.CloudFoundry cloudFoundry) {
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
        CloudFoundryOperations operations = DefaultCloudFoundryOperations.builder()
                .cloudFoundryClient(cloudFoundryClient)
                .organization(cloudFoundry.getOrganization())
                .space(cloudFoundry.getSpace())
                .build();
        return new CloudFoundryConnection(cloudFoundryClient, operations);
    }

    private RunApplicationTaskRequest buildRunTaskRequest(TaskLaunchRequest request, String command) {
        return RunApplicationTaskRequest.builder()
                .applicationName(request.taskHostApp())
                .command(command)
                .taskName(request.taskName())
                .memory(request.memoryMb())
                .disk(request.diskMb())
                .build();
    }

    private Duration launchTimeout(TaskLaunchRequest request) {
        return request.timeout() == null ? properties.getTimeout() : request.timeout();
    }

    private String resolveCommand(CloudFoundryConnection connection, TaskLaunchProperties.CloudFoundry cloudFoundry,
            TaskLaunchRequest request) {
        String command = request.command();
        if (command == null || command.isBlank()) {
            throw new IllegalStateException("Cloud Foundry task command must not be blank.");
        }
        if (!command.contains(DEFAULT_COMMAND_PLACEHOLDER)) {
            return command;
        }
        String defaultCommand = defaultCommandResolver.resolve(connection, cloudFoundry, request);
        if (defaultCommand == null || defaultCommand.isBlank()) {
            throw new IllegalStateException("Current Cloud Foundry droplet default command is blank.");
        }
        return command.replace(DEFAULT_COMMAND_PLACEHOLDER, defaultCommand);
    }

    private static String loadDefaultStartCommand(CloudFoundryConnection connection,
            TaskLaunchProperties.CloudFoundry cloudFoundry, TaskLaunchRequest request) {
        return resolveApplicationId(connection.client(), cloudFoundry, request.taskHostApp())
                .flatMap(applicationId -> connection.client()
                        .applicationsV3()
                        .getCurrentDroplet(GetApplicationCurrentDropletRequest.builder()
                                .applicationId(applicationId)
                                .build()))
                .map(currentDroplet -> extractWebProcessCommand(
                        request.taskHostApp(),
                        currentDroplet.getProcessTypes()))
                .block(resolveMetadataTimeout(request));
    }

    private static Duration resolveMetadataTimeout(TaskLaunchRequest request) {
        return request.timeout() == null ? Duration.ofMinutes(1) : request.timeout();
    }

    private static Mono<String> resolveApplicationId(CloudFoundryClient cloudFoundryClient,
            TaskLaunchProperties.CloudFoundry cloudFoundry, String applicationName) {
        return resolveOrganizationId(cloudFoundryClient, cloudFoundry.getOrganization())
                .flatMap(organizationId -> resolveSpaceId(cloudFoundryClient, organizationId, cloudFoundry.getSpace()))
                .flatMap(spaceId -> cloudFoundryClient.applicationsV3()
                        .list(ListApplicationsRequest.builder()
                                .name(applicationName)
                                .spaceId(spaceId)
                                .build()))
                .map(response -> singleApplicationId(response.getResources(), applicationName));
    }

    private static Mono<String> resolveOrganizationId(CloudFoundryClient cloudFoundryClient, String organizationName) {
        return cloudFoundryClient.organizationsV3()
                .list(ListOrganizationsRequest.builder()
                        .name(organizationName)
                        .build())
                .map(response -> singleOrganizationId(response.getResources(), organizationName));
    }

    private static Mono<String> resolveSpaceId(CloudFoundryClient cloudFoundryClient, String organizationId,
            String spaceName) {
        return cloudFoundryClient.spacesV3()
                .list(ListSpacesRequest.builder()
                        .organizationId(organizationId)
                        .name(spaceName)
                        .build())
                .map(response -> singleSpaceId(response.getResources(), spaceName));
    }

    private static String extractWebProcessCommand(String applicationName, Map<String, String> processTypes) {
        if (processTypes == null || processTypes.get(WEB_PROCESS_TYPE) == null
                || processTypes.get(WEB_PROCESS_TYPE).isBlank()) {
            throw new IllegalStateException("Current droplet for app " + applicationName
                    + " does not expose a web process command.");
        }
        return processTypes.get(WEB_PROCESS_TYPE);
    }

    private static String singleApplicationId(List<ApplicationResource> resources, String applicationName) {
        if (resources == null || resources.isEmpty()) {
            throw new IllegalStateException("Cloud Foundry app " + applicationName + " does not exist.");
        }
        if (resources.size() > 1) {
            throw new IllegalStateException("Cloud Foundry app name " + applicationName
                    + " resolved to multiple applications.");
        }
        return resources.get(0).getId();
    }

    private static String singleOrganizationId(List<OrganizationResource> resources, String organizationName) {
        if (resources == null || resources.isEmpty()) {
            throw new IllegalStateException("Cloud Foundry organization " + organizationName + " does not exist.");
        }
        return resources.get(0).getId();
    }

    private static String singleSpaceId(List<SpaceResource> resources, String spaceName) {
        if (resources == null || resources.isEmpty()) {
            throw new IllegalStateException("Cloud Foundry space " + spaceName + " does not exist.");
        }
        return resources.get(0).getId();
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

    record CloudFoundryConnection(CloudFoundryClient client, CloudFoundryOperations operations) {
    }

    @FunctionalInterface
    interface DefaultCommandResolver {
        String resolve(CloudFoundryConnection connection, TaskLaunchProperties.CloudFoundry cloudFoundry,
                TaskLaunchRequest request);
    }
}
