package customer.batchimportcat.batch.dynamic.types;

import java.util.List;
import java.util.Map;

import customer.batchimportcat.batch.dynamic.DynamicDataFactory;
import customer.batchimportcat.batch.dynamic.dto.DynamicNode;
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
public class BatchImportProcessContext {
    private final String fileUUID;
    private final DynamicImportConfiguration configuration;
    private final Map<String, DynamicTableHandle> handlesByStructureUUID;
    private final Map<String, DynamicTableHandle> handlesByStructureName;
    private final DynamicDataFactory dataFactory;

    public DynamicTableHandle getRequiredHandle(String structureUUID) {
        DynamicTableHandle handle = handlesByStructureUUID.get(structureUUID);
        if (handle == null) {
            throw new IllegalArgumentException("No dynamic handle found for structure UUID " + structureUUID + ".");
        }
        return handle;
    }

    public DynamicTableHandle getRequiredHandleByName(String structureName) {
        DynamicTableHandle handle = handlesByStructureName.get(structureName);
        if (handle == null) {
            throw new IllegalArgumentException("No dynamic handle found for structure name " + structureName + ".");
        }
        return handle;
    }

    public BatchImportProcessPayload createPayload(List<DynamicNode> rootNodes) {
        return dataFactory.createRootPayload(rootNodes, this);
    }
}
