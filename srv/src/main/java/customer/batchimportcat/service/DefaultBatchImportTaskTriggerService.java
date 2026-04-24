package customer.batchimportcat.service;

import org.springframework.stereotype.Service;

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

@Service
public class DefaultBatchImportTaskTriggerService implements BatchImportTaskTriggerService {
    private final BatchImportFileCqnService batchImportFileCqnService;
    private final BatchImportConfigCqnService batchImportConfigCqnService;
    private final ProcessorArtifactCqnService processorArtifactCqnService;
    private final BatchImportExecutionCqnService batchImportExecutionCqnService;
    private final TaskLaunchRequestFactory taskLaunchRequestFactory;
    private final TaskLaunchService taskLaunchService;

    public DefaultBatchImportTaskTriggerService(BatchImportFileCqnService batchImportFileCqnService,
            BatchImportConfigCqnService batchImportConfigCqnService,
            ProcessorArtifactCqnService processorArtifactCqnService,
            BatchImportExecutionCqnService batchImportExecutionCqnService,
            TaskLaunchRequestFactory taskLaunchRequestFactory,
            TaskLaunchService taskLaunchService) {
        this.batchImportFileCqnService = batchImportFileCqnService;
        this.batchImportConfigCqnService = batchImportConfigCqnService;
        this.processorArtifactCqnService = processorArtifactCqnService;
        this.batchImportExecutionCqnService = batchImportExecutionCqnService;
        this.taskLaunchRequestFactory = taskLaunchRequestFactory;
        this.taskLaunchService = taskLaunchService;
    }

    @Override
    public BatchImportExecutionRef trigger(String fileUUID) {
        String configUUID = batchImportFileCqnService.loadRequiredConfigUUID(fileUUID);
        BatchImportConfigLaunchContext configContext = batchImportConfigCqnService.loadRequiredLaunchContext(configUUID);
        BatchImportFileLaunchContext fileContext = new BatchImportFileLaunchContext(
                fileUUID,
                configUUID,
                configContext.processKey(),
                configContext.object(),
                configContext.objectName());
        ProcessorArtifactBinding artifact = processorArtifactCqnService
                .getRequiredEnabledArtifact(configContext.processKey());
        String executionUUID = batchImportExecutionCqnService.createSubmittedExecution(fileContext, artifact,
            taskLaunchRequestFactory.taskHostApp(),
            taskLaunchRequestFactory.launcherType());
        try {
            batchImportFileCqnService.markQueued(fileUUID, null);
            TaskLaunchRequest request = taskLaunchRequestFactory.create(executionUUID, fileContext, artifact);
            TaskLaunchResult launchResult = taskLaunchService.launch(request);
            if (!launchResult.accepted()) {
                String failureReason = firstNonBlank(launchResult.failureReason(), launchResult.rawState(),
                        "Task launch was rejected.");
                batchImportExecutionCqnService.markLaunchFailed(executionUUID, failureReason);
                batchImportFileCqnService.markError(fileUUID, null);
                return new BatchImportExecutionRef(executionUUID, fileUUID, fileContext.processKey(),
                        TaskState.FAILED.value());
            }
            batchImportExecutionCqnService.markLaunchAccepted(executionUUID, launchResult);
            return new BatchImportExecutionRef(executionUUID, fileUUID, fileContext.processKey(),
                    TaskState.SUBMITTED.value());
        } catch (Exception exception) {
            batchImportExecutionCqnService.markLaunchFailed(executionUUID, summarizeFailure(exception));
            batchImportFileCqnService.markError(fileUUID, null);
            return new BatchImportExecutionRef(executionUUID, fileUUID, fileContext.processKey(),
                    TaskState.FAILED.value());
        }
    }

    @Override
    public BatchImportExecutionRef retry(String executionUUID) {
        String fileUUID = batchImportExecutionCqnService.loadRequiredFileUUID(executionUUID);
        return trigger(fileUUID);
    }

    private String summarizeFailure(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
