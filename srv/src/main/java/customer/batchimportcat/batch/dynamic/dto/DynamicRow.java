package customer.batchimportcat.batch.dynamic.dto;

import java.io.Serializable;
import java.util.LinkedHashMap;

public class DynamicRow extends LinkedHashMap<String, Object> implements Serializable {
    private final String structureUUID;
    private final String structureName;
    private final int lineNumber;

    public DynamicRow(String structureUUID, String structureName, int lineNumber) {
        this.structureUUID = structureUUID;
        this.structureName = structureName;
        this.lineNumber = lineNumber;
    }

    public String getStructureUUID() {
        return structureUUID;
    }

    public String getStructureName() {
        return structureName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

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
