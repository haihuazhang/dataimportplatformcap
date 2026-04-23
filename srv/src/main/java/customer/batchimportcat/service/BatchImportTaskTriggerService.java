package customer.batchimportcat.service;

import customer.batchimportcat.model.BatchImportExecutionRef;

public interface BatchImportTaskTriggerService {
    BatchImportExecutionRef trigger(String fileUUID);

    BatchImportExecutionRef retry(String executionUUID);
}
