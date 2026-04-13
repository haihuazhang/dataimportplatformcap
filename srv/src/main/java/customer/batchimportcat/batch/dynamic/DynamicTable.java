package customer.batchimportcat.batch.dynamic;

import java.io.Serializable;
import java.util.ArrayList;

public class DynamicTable extends ArrayList<DynamicRow> implements Serializable {
    private final String structureUUID;
    private final String structureName;

    public DynamicTable(String structureUUID, String structureName) {
        this.structureUUID = structureUUID;
        this.structureName = structureName;
    }

    public String getStructureUUID() {
        return structureUUID;
    }

    public String getStructureName() {
        return structureName;
    }
}
