package customer.batchimportcat.batch.dynamic;

import java.io.Serializable;
import java.util.List;

public record DynamicTableHandle(
        String structureUUID,
        String structureName,
        List<DynamicFieldDefinition> fields,
        List<String> childStructureNames) implements Serializable {
}
