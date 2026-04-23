package customer.batchimportcat.batch.dynamic.types;

import java.io.Serializable;

public record BatchImportProcessMessage(long line, String type, String code, String message, String details)
        implements Serializable {
}
