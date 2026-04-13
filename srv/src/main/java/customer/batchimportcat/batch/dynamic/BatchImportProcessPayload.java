package customer.batchimportcat.batch.dynamic;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

public record BatchImportProcessPayload(
        Map<String, DynamicTable> rootTablesByStructureUUID,
        Map<String, DynamicTable> rootTablesByStructureName) implements Serializable {
    public DynamicTable getRequiredRootTable(String structureUUID) {
        DynamicTable table = rootTablesByStructureUUID.get(structureUUID);
        if (table == null) {
            throw new IllegalArgumentException("No root table found for structure UUID " + structureUUID + ".");
        }
        return table;
    }

    public DynamicTable getRequiredRootTableByName(String structureName) {
        DynamicTable table = rootTablesByStructureName.get(structureName);
        if (table == null) {
            throw new IllegalArgumentException("No root table found for structure name " + structureName + ".");
        }
        return table;
    }

    public Collection<DynamicTable> rootTables() {
        return rootTablesByStructureUUID.values();
    }

    public boolean isEmpty() {
        return rootTablesByStructureUUID.isEmpty();
    }
}
