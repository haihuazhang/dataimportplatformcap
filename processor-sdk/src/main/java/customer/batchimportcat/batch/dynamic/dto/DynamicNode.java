package customer.batchimportcat.batch.dynamic.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DynamicNode implements Serializable {
    private final String structureUUID;
    private final String structureName;
    private final int lineNumber;
    private final LinkedHashMap<String, Object> fields;
    private final LinkedHashMap<String, List<DynamicNode>> children;

    public DynamicNode(String structureUUID, String structureName, int lineNumber) {
        this(structureUUID, structureName, lineNumber, new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    public void putField(String fieldName, Object value) {
        fields.put(fieldName, value);
    }

    public void addChild(String childName, DynamicNode node) {
        children.computeIfAbsent(childName, ignored -> new ArrayList<>()).add(node);
    }

    public DynamicNode copyShallow() {
        return new DynamicNode(structureUUID, structureName, lineNumber, new LinkedHashMap<>(fields),
                new LinkedHashMap<>());
    }

    public Map<String, Object> asSerializableMap() {
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("structureUUID", structureUUID);
        data.put("structureName", structureName);
        data.put("lineNumber", lineNumber);
        data.put("fields", fields);
        data.put("children", children);
        return data;
    }
}
