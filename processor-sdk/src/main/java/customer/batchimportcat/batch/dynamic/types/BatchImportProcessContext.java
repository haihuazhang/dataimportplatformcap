package customer.batchimportcat.batch.dynamic.types;

import java.util.Map;

import customer.batchimportcat.batch.runtime.ProcessorRuntime;
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
    private final ProcessorRuntime runtime;

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
}
