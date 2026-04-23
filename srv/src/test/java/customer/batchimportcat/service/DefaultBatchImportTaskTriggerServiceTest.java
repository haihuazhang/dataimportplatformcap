package customer.batchimportcat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import customer.batchimportcat.batch.runtime.TaskState;
import customer.batchimportcat.model.BatchImportConfigLaunchContext;
import customer.batchimportcat.model.BatchImportExecutionRef;
import customer.batchimportcat.model.BatchImportFileLaunchContext;
import customer.batchimportcat.model.ProcessorArtifactBinding;
import customer.batchimportcat.model.TaskLaunchRequest;
import customer.batchimportcat.model.TaskLaunchRequestFactory;
import customer.batchimportcat.model.TaskLaunchResult;
import customer.batchimportcat.service.cqn.BatchImportConfigCqnService;
import customer.batchimportcat.service.cqn.BatchImportExecutionCqnService;
import customer.batchimportcat.service.cqn.BatchImportFileCqnService;
import customer.batchimportcat.service.cqn.ProcessorArtifactCqnService;
import customer.batchimportcat.service.tasklaunch.TaskLaunchService;

class DefaultBatchImportTaskTriggerServiceTest {
    @Test
    void triggerMarksQueuedAndAcceptedWhenLaunchSucceeds() {
        List<String> calls = new ArrayList<>();
        RecordingBatchImportFileCqnService batchImportFileCqnService = new RecordingBatchImportFileCqnService(
                "config-1",
                calls);
        RecordingBatchImportConfigCqnService batchImportConfigCqnService = new RecordingBatchImportConfigCqnService(
                new BatchImportConfigLaunchContext("config-1", "PROCESS_A", "OBJ", "Object A"));
        RecordingProcessorArtifactCqnService processorArtifactCqnService = new RecordingProcessorArtifactCqnService(
                new ProcessorArtifactBinding("artifact-1", "PROCESS_A", "1.0.0", "Artifact", "abc", "demo.Entry"));
        RecordingBatchImportExecutionCqnService batchImportExecutionCqnService = new RecordingBatchImportExecutionCqnService(
                calls,
                "exec-1");
        RecordingTaskLaunchRequestFactory requestFactory = new RecordingTaskLaunchRequestFactory();
        TaskLaunchService launchService = request -> {
            calls.add("launch:" + request.executionUUID() + ":" + request.fileUUID());
            return new TaskLaunchResult(true, "local", "task-1", request.taskName(), Instant.now(), "SUBMITTED", null);
        };

        DefaultBatchImportTaskTriggerService service = new DefaultBatchImportTaskTriggerService(
                batchImportFileCqnService,
                batchImportConfigCqnService,
                processorArtifactCqnService,
                batchImportExecutionCqnService,
                requestFactory,
                launchService);

        BatchImportExecutionRef ref = service.trigger("file-1");

        assertEquals("exec-1", ref.executionUUID());
        assertEquals(TaskState.SUBMITTED.value(), ref.taskState());
        assertEquals(List.of(
                "create:exec-1:file-1:artifact-1",
                "queued:file-1:null",
                "launch:exec-1:file-1",
                "accepted:exec-1:task-1"),
                calls);
    }

    @Test
    void triggerMarksExecutionAndFileErrorWhenLaunchFails() {
        List<String> calls = new ArrayList<>();
        RecordingBatchImportFileCqnService batchImportFileCqnService = new RecordingBatchImportFileCqnService(
                "config-2",
                calls);
        RecordingBatchImportConfigCqnService batchImportConfigCqnService = new RecordingBatchImportConfigCqnService(
                new BatchImportConfigLaunchContext("config-2", "PROCESS_B", "OBJ", "Object B"));
        RecordingProcessorArtifactCqnService processorArtifactCqnService = new RecordingProcessorArtifactCqnService(
                new ProcessorArtifactBinding("artifact-2", "PROCESS_B", "2.0.0", "Artifact", "def", "demo.Entry"));
        RecordingBatchImportExecutionCqnService batchImportExecutionCqnService = new RecordingBatchImportExecutionCqnService(
                calls,
                "exec-2");
        RecordingTaskLaunchRequestFactory requestFactory = new RecordingTaskLaunchRequestFactory();
        TaskLaunchService launchService = request -> new TaskLaunchResult(
                false,
                "local",
                null,
                request.taskName(),
                Instant.now(),
                "launcher rejected",
                "launcher rejected");

        DefaultBatchImportTaskTriggerService service = new DefaultBatchImportTaskTriggerService(
                batchImportFileCqnService,
                batchImportConfigCqnService,
                processorArtifactCqnService,
                batchImportExecutionCqnService,
                requestFactory,
                launchService);

        BatchImportExecutionRef ref = service.trigger("file-2");

        assertEquals("exec-2", ref.executionUUID());
        assertEquals(TaskState.FAILED.value(), ref.taskState());
        assertEquals(List.of(
                "create:exec-2:file-2:artifact-2",
                "queued:file-2:null",
                "failed:exec-2:launcher rejected",
                "error:file-2:null"),
                calls);
    }

    private static final class RecordingBatchImportFileCqnService extends BatchImportFileCqnService {
        private final String configUUID;
        private final List<String> calls;

        private RecordingBatchImportFileCqnService(String configUUID, List<String> calls) {
            super(null);
            this.configUUID = configUUID;
            this.calls = calls;
        }

        @Override
        public String loadRequiredConfigUUID(String fileUUID) {
            return configUUID;
        }

        @Override
        public void markQueued(String fileUUID, Long jobInstanceId) {
            calls.add("queued:" + fileUUID + ":" + jobInstanceId);
        }

        @Override
        public void markError(String fileUUID, Long jobInstanceId) {
            calls.add("error:" + fileUUID + ":" + jobInstanceId);
        }
    }

    private static final class RecordingBatchImportConfigCqnService extends BatchImportConfigCqnService {
        private final BatchImportConfigLaunchContext context;

        private RecordingBatchImportConfigCqnService(BatchImportConfigLaunchContext context) {
            super(null, null);
            this.context = context;
        }

        @Override
        public BatchImportConfigLaunchContext loadRequiredLaunchContext(String configUUID) {
            return context;
        }
    }

    private static final class RecordingProcessorArtifactCqnService extends ProcessorArtifactCqnService {
        private final ProcessorArtifactBinding artifactBinding;

        private RecordingProcessorArtifactCqnService(ProcessorArtifactBinding artifactBinding) {
            super(null);
            this.artifactBinding = artifactBinding;
        }

        @Override
        public ProcessorArtifactBinding getRequiredEnabledArtifact(String processKey) {
            return artifactBinding;
        }
    }

    private static final class RecordingBatchImportExecutionCqnService extends BatchImportExecutionCqnService {
        private final List<String> calls;
        private final String executionUUID;

        private RecordingBatchImportExecutionCqnService(List<String> calls, String executionUUID) {
            super(null);
            this.calls = calls;
            this.executionUUID = executionUUID;
        }

        @Override
        public String createSubmittedExecution(BatchImportFileLaunchContext fileContext, ProcessorArtifactBinding artifact,
                String taskHostApp) {
            calls.add("create:" + executionUUID + ":" + fileContext.fileUUID() + ":" + artifact.id());
            return executionUUID;
        }

        @Override
        public void markLaunchAccepted(String executionUUID, TaskLaunchResult launchResult) {
            calls.add("accepted:" + executionUUID + ":" + launchResult.platformTaskId());
        }

        @Override
        public void markLaunchFailed(String executionUUID, String failureReason) {
            calls.add("failed:" + executionUUID + ":" + failureReason);
        }
    }

    private static final class RecordingTaskLaunchRequestFactory extends TaskLaunchRequestFactory {
        private RecordingTaskLaunchRequestFactory() {
            super(defaultProperties());
        }

        @Override
        public TaskLaunchRequest create(String executionUUID, BatchImportFileLaunchContext fileContext,
                ProcessorArtifactBinding artifact) {
            return new TaskLaunchRequest(
                    executionUUID,
                    fileContext.fileUUID(),
                    fileContext.processKey(),
                    artifact.id(),
                    artifact.version(),
                    artifact.artifactChecksum(),
                    "batchimportcat-batch-task",
                    "task-" + executionUUID,
                    "echo launch",
                    "local",
                    512,
                    512,
                    Map.of(),
                    Duration.ofMinutes(5));
        }

        @Override
        public String taskHostApp() {
            return "batchimportcat-batch-task";
        }

        private static customer.batchimportcat.model.TaskLaunchProperties defaultProperties() {
            return new customer.batchimportcat.model.TaskLaunchProperties();
        }
    }
}
