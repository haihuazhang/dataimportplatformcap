package customer.batchimportcat.model;

public record BatchImportFileLaunchContext(
        String fileUUID,
        String configUUID,
        String processKey,
        String object,
        String objectName) {
}
