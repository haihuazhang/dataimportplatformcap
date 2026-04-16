package customer.batchimportcat.batch.dynamic.dto;

import java.util.ArrayList;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DynamicTable extends ArrayList<DynamicRow> {
    private final String structureUUID;
    private final String structureName;
}
