package customer.batchimportcat.model;

public record BatchImportExecutionRef(
        String executionUUID,
        String fileUUID,
        String processKey,
        String taskState) {
}
