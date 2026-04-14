package customer.batchimportcat.batch.dynamic;

import java.io.Serializable;
import java.util.List;

import customer.batchimportcat.batch.dynamic.types.DynamicFieldDefinition;

public record DynamicTableHandle(
        String structureUUID,
        String structureName,
        List<DynamicFieldDefinition> fields,
        List<String> childStructureNames) implements Serializable {
}
