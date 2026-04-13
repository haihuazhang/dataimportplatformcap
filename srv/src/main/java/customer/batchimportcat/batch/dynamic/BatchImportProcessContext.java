package customer.batchimportcat.batch.dynamic;

import java.util.List;
import java.util.Map;

public record BatchImportProcessContext(
        String fileUUID,
        DynamicImportConfiguration configuration,
        Map<String, DynamicTableHandle> handlesByStructureUUID,
        Map<String, DynamicTableHandle> handlesByStructureName,
        DynamicDataFactory dataFactory) {
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
