package customer.batchimportcat.batch.dynamic.dto;

import java.util.ArrayList;

import lombok.Getter;

@Getter
public class DynamicTable extends ArrayList<DynamicRow> {
    private final String structureUUID;
    private final String structureName;

    public DynamicTable(String structureUUID, String structureName) {
        this.structureUUID = structureUUID;
        this.structureName = structureName;
    }

}
