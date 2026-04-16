package customer.batchimportcat.batch.dynamic.dto;

import java.util.LinkedHashMap;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
// Safety option (keep disabled for now):
// @JsonIgnoreProperties({ "structureUUID", "structureName", "lineNumber" })
public class DynamicRow extends LinkedHashMap<String, Object> {
    // @JsonIgnore
    private final String structureUUID;
    // @JsonIgnore
    private final String structureName;
    // @JsonIgnore
    private final int lineNumber;

    public DynamicTable getRequiredChildTable(String childStructureName) {
        Object childTable = get(childStructureName);
        if (childTable == null) {
            throw new IllegalArgumentException(
                    "No child table found for structure " + childStructureName + " in row " + lineNumber + ".");
        }
        if (!(childTable instanceof DynamicTable dynamicTable)) {
            throw new IllegalStateException(
                    "Child value " + childStructureName + " is not a DynamicTable in row " + lineNumber + ".");
        }
        return dynamicTable;
    }
}
