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
public class DynamicImportConfiguration implements Serializable {
    private final String id;
    private final String object;
    private final String objectName;
    private final String processKey;
    private final String implementedByClass;
    private final String legacyStructName;
    private final String legacySheetName;
    private final int legacyStartLine;
    private final String legacyStartColumn;
    private final List<DynamicStructureDefinition> structures;

    public List<DynamicStructureDefinition> sortedStructures() {
        return structures.stream()
                .sorted(Comparator.comparing(DynamicStructureDefinition::rootNode).reversed()
                        .thenComparing(DynamicStructureDefinition::sheetName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<DynamicStructureDefinition> rootStructures() {
        return sortedStructures().stream().filter(DynamicStructureDefinition::rootNode).toList();
    }
}
