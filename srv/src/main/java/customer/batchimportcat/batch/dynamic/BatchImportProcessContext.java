package customer.batchimportcat.batch.dynamic;

import java.io.Serializable;

public record BatchImportProcessContext(String fileUUID, DynamicImportConfiguration configuration) implements Serializable {
}
