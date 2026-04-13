package customer.batchimportcat.batch.dynamic;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

public record DynamicImportConfiguration(
        String id,
        String object,
        String objectName,
        String processKey,
        String implementedByClass,
        String legacyStructName,
        String legacySheetName,
        int legacyStartLine,
        String legacyStartColumn,
        List<DynamicStructureDefinition> structures) implements Serializable {

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
