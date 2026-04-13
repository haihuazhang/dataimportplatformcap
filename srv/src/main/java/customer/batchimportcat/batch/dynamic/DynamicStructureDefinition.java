package customer.batchimportcat.batch.dynamic;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

public record DynamicStructureDefinition(
        String id,
        String configUUID,
        boolean rootNode,
        String sheetName,
        String sheetNameUp,
        int startLine,
        String startColumn,
        boolean hasFieldnameLine,
        boolean hasDescLine,
        List<DynamicFieldDefinition> fields) implements Serializable {

    public List<DynamicFieldDefinition> sortedFields() {
        return fields.stream()
                .sorted(Comparator.comparingLong(DynamicFieldDefinition::sequence)
                        .thenComparing(DynamicFieldDefinition::fieldName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
