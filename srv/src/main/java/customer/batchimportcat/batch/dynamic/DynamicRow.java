package customer.batchimportcat.batch.dynamic;

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
}
