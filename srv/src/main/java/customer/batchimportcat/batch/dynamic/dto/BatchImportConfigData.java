package customer.batchimportcat.batch.dynamic.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public record BatchImportConfigData(
        Map<String, Serializable> configData,
        List<Map<String, Serializable>> structureRows,
        List<Map<String, Serializable>> fieldRows) {
}