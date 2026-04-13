package customer.batchimportcat.batch.dynamic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class DynamicDataFactory {
    public Map<String, DynamicTableHandle> createHandles(DynamicImportConfiguration configuration) {
        Map<String, List<String>> childNamesByParentSheet = new LinkedHashMap<>();
        for (DynamicStructureDefinition structure : configuration.sortedStructures()) {
            if (structure.sheetNameUp() != null && !structure.sheetNameUp().isBlank()) {
                childNamesByParentSheet.computeIfAbsent(structure.sheetNameUp(), ignored -> new ArrayList<>())
                        .add(structure.sheetName());
            }
        }

        Map<String, DynamicTableHandle> handles = new LinkedHashMap<>();
        for (DynamicStructureDefinition structure : configuration.sortedStructures()) {
            handles.put(structure.id(), new DynamicTableHandle(
                    structure.id(),
                    structure.sheetName(),
                    List.copyOf(structure.sortedFields()),
                    List.copyOf(childNamesByParentSheet.getOrDefault(structure.sheetName(), List.of()))));
        }
        return Map.copyOf(handles);
    }

    public DynamicRow createRow(DynamicTableHandle handle, DynamicNode node, BatchImportProcessContext context) {
        DynamicRow row = new DynamicRow(handle.structureUUID(), handle.structureName(), node.getLineNumber());
        for (DynamicFieldDefinition field : handle.fields()) {
            row.put(field.fieldName(), null);
        }
        for (String childStructureName : handle.childStructureNames()) {
            DynamicTableHandle childHandle = context.getRequiredHandleByName(childStructureName);
            row.put(childStructureName, createTable(childHandle));
        }
        for (DynamicFieldDefinition field : handle.fields()) {
            row.put(field.fieldName(), node.getFields().get(field.fieldName()));
        }
        for (String childStructureName : handle.childStructureNames()) {
            List<DynamicNode> childNodes = node.getChildren().getOrDefault(childStructureName, List.of());
            row.put(childStructureName, createTable(childStructureName, childNodes, context));
        }
        return row;
    }

    public DynamicTable createTable(DynamicTableHandle handle) {
        return new DynamicTable(handle.structureUUID(), handle.structureName());
    }

    public DynamicTable createTable(DynamicTableHandle handle, List<DynamicNode> nodes, BatchImportProcessContext context) {
        DynamicTable table = createTable(handle);
        for (DynamicNode node : nodes) {
            table.add(createRow(handle, node, context));
        }
        return table;
    }

    public DynamicTable createTable(String structureName, List<DynamicNode> nodes, BatchImportProcessContext context) {
        DynamicTableHandle handle = context.getRequiredHandleByName(structureName);
        return createTable(handle, nodes, context);
    }

    public BatchImportProcessPayload createRootPayload(List<DynamicNode> rootNodes, BatchImportProcessContext context) {
        Map<String, DynamicTable> rootTablesByStructureUUID = new LinkedHashMap<>();
        Map<String, DynamicTable> rootTablesByStructureName = new LinkedHashMap<>();

        for (DynamicNode rootNode : rootNodes) {
            DynamicTableHandle handle = context.getRequiredHandle(rootNode.getStructureUUID());
            DynamicTable table = rootTablesByStructureUUID.computeIfAbsent(handle.structureUUID(), ignored -> {
                DynamicTable created = createTable(handle);
                rootTablesByStructureName.put(handle.structureName(), created);
                return created;
            });
            table.add(createRow(handle, rootNode, context));
        }
        return new BatchImportProcessPayload(Map.copyOf(rootTablesByStructureUUID), Map.copyOf(rootTablesByStructureName));
    }
}
