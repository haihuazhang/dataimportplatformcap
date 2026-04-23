package customer.batchimportcat.model;

public record BatchImportConfigLaunchContext(
        String configUUID,
        String processKey,
        String object,
        String objectName) {
}
