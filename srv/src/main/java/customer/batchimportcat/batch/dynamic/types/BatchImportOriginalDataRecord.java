package customer.batchimportcat.batch.dynamic.types;

import java.io.Serializable;

public record BatchImportOriginalDataRecord(Long line, String structureName, String dataJson)
        implements Serializable {
}
