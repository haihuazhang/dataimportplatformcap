package customer.batchimportcat.batch.dynamic.types;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
@Accessors(fluent = true)
public class DynamicStructureDefinition implements Serializable {
        private final String id;
        private final String configUUID;
        private final boolean rootNode;
        private final String sheetName;
        private final String sheetNameUp;
        private final int startLine;
        private final String startColumn;
        private final boolean hasFieldnameLine;
        private final boolean hasDescLine;
        private final List<DynamicFieldDefinition> fields;

    public List<DynamicFieldDefinition> sortedFields() {
        return fields.stream()
                .sorted(Comparator.comparingLong(DynamicFieldDefinition::sequence)
                        .thenComparing(DynamicFieldDefinition::fieldName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
