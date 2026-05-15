package customer.batchimportcat.service.tasklaunch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Map;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.Applications;
import org.cloudfoundry.operations.applications.RunApplicationTaskRequest;
import org.cloudfoundry.operations.applications.Task;
import org.cloudfoundry.operations.applications.TaskState;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import customer.batchimportcat.model.TaskLaunchProperties;
import customer.batchimportcat.model.TaskLaunchRequest;
import customer.batchimportcat.model.TaskLaunchResult;
import reactor.core.publisher.Mono;

class CloudFoundryTaskLaunchServiceTest {
    @Test
    void springCreatesCloudFoundryLauncherBean() {
        new ApplicationContextRunner()
                .withBean(TaskLaunchProperties.class)
                .withBean(CloudFoundryTaskLaunchService.class)
                .withPropertyValues("batchimport.task.launcher-type=cloudfoundry")
                .run(context -> assertTrue(context.containsBean("cloudFoundryTaskLaunchService")));
    }

    @Test
    void launchCreatesCloudFoundryTaskThroughJavaClient() {
        CloudFoundryOperations operations = mock(CloudFoundryOperations.class);
        Applications applications = mock(Applications.class);
        when(operations.applications()).thenReturn(applications);
        when(applications.runTask(any(RunApplicationTaskRequest.class))).thenReturn(Mono.just(Task.builder()
                .command("run command")
                .name("batch-import-123456")
                .sequenceId(7)
                .startTime("2026-05-14T00:00:00Z")
                .state(TaskState.PENDING)
                .build()));
        TaskLaunchProperties properties = cloudFoundryProperties();
        CloudFoundryTaskLaunchService service = new CloudFoundryTaskLaunchService(
                properties,
                cloudFoundry -> new CloudFoundryTaskLaunchService.CloudFoundryConnection(
                        mock(CloudFoundryClient.class),
                        operations));

        TaskLaunchResult result = service.launch(request());

        assertTrue(result.accepted());
        assertEquals("7", result.platformTaskId());
        assertEquals("batch-import-123456", result.platformTaskName());
        assertEquals("run command", result.command());
        assertEquals("PENDING", result.rawState());
        verify(applications).runTask(RunApplicationTaskRequest.builder()
                .applicationName("batchimportcat-batch-task")
                .command("run command")
                .taskName("batch-import-123456")
                .memory(1024)
                .disk(1024)
                .build());
    }

    @Test
    void launchFailsBeforeCallingCloudFoundryWhenPasswordIsMissing() {
        TaskLaunchProperties properties = cloudFoundryProperties();
        properties.getCloudfoundry().setPassword("");
        CloudFoundryTaskLaunchService service = new CloudFoundryTaskLaunchService(properties, cloudFoundry -> {
            throw new AssertionError("Cloud Foundry client should not be created without password.");
        });

        TaskLaunchResult result = service.launch(request());

        assertFalse(result.accepted());
        assertEquals("cloudfoundry", result.launcherType());
        assertEquals("run command", result.command());
        assertEquals("batchimport.task.cloudfoundry.password is required for Cloud Foundry task launch.",
                result.failureReason());
    }

    @Test
    void launchReplacesDefaultCommandPlaceholderFromCurrentDropletCommand() {
        CloudFoundryOperations operations = mock(CloudFoundryOperations.class);
        Applications applications = mock(Applications.class);
        when(operations.applications()).thenReturn(applications);
        when(applications.runTask(any(RunApplicationTaskRequest.class))).thenReturn(Mono.just(Task.builder()
                .command("default command --fileUUID=file-1 --executionUUID=execution-1")
                .name("batch-import-123456")
                .sequenceId(8)
                .startTime("2026-05-14T00:00:00Z")
                .state(TaskState.PENDING)
                .build()));
        TaskLaunchProperties properties = cloudFoundryProperties();
        CloudFoundryTaskLaunchService service = new CloudFoundryTaskLaunchService(
                properties,
                cloudFoundry -> new CloudFoundryTaskLaunchService.CloudFoundryConnection(
                        mock(CloudFoundryClient.class),
                        operations),
                (connection, cloudFoundry, request) -> "default command");

        TaskLaunchResult result = service.launch(requestWithDefaultCommandPlaceholder());

        assertTrue(result.accepted());
        assertEquals("default command --fileUUID=file-1 --executionUUID=execution-1", result.command());
        verify(applications).runTask(RunApplicationTaskRequest.builder()
                .applicationName("batchimportcat-batch-task")
                .command("default command --fileUUID=file-1 --executionUUID=execution-1")
                .taskName("batch-import-123456")
                .memory(1024)
                .disk(1024)
                .build());
    }

    private static TaskLaunchProperties cloudFoundryProperties() {
        TaskLaunchProperties properties = new TaskLaunchProperties();
        properties.setLauncherType("cloudfoundry");
        properties.getCloudfoundry().setApiHost("https://api.cf.us10-001.hana.ondemand.com");
        properties.getCloudfoundry().setOrganization("a83421e9trial");
        properties.getCloudfoundry().setSpace("dev");
        properties.getCloudfoundry().setUsername("user@example.com");
        properties.getCloudfoundry().setPassword("password");
        return properties;
    }

    private static TaskLaunchRequest request() {
        return new TaskLaunchRequest(
                "execution-1",
                "file-1",
                "PROCESS_A",
                "artifact-1",
                "1.0.0",
                "abc",
                "batchimportcat-batch-task",
                "batch-import-123456",
                "run command",
                "cloudfoundry",
                1024,
                1024,
                Map.of(),
                Duration.ofMinutes(5));
    }

    private static TaskLaunchRequest requestWithDefaultCommandPlaceholder() {
        return new TaskLaunchRequest(
                "execution-1",
                "file-1",
                "PROCESS_A",
                "artifact-1",
                "1.0.0",
                "abc",
                "batchimportcat-batch-task",
                "batch-import-123456",
                "{defaultCommand} --fileUUID=file-1 --executionUUID=execution-1",
                "cloudfoundry",
                1024,
                1024,
                Map.of(),
                Duration.ofMinutes(5));
    }
}
