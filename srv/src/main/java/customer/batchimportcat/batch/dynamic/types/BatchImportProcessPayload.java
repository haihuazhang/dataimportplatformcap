package customer.batchimportcat.batch.dynamic.types;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import customer.batchimportcat.batch.dynamic.dto.DynamicTable;
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
public class BatchImportProcessPayload implements Serializable {
    private final Map<String, DynamicTable> rootTablesByStructureUUID;
    private final Map<String, DynamicTable> rootTablesByStructureName;

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
