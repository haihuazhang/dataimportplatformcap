package customer.batchimportcat.batch.dynamic;

import java.io.Serializable;

public record DynamicFieldDefinition(
        String id,
        String configUUID,
        String structureUUID,
        String fieldName,
        String fieldDescription,
        boolean keyField,
        long sequence,
        boolean foreignField,
        String foreignFieldName,
        Integer fieldLength,
        String fieldType,
        Integer fieldDecimal) implements Serializable {
}
