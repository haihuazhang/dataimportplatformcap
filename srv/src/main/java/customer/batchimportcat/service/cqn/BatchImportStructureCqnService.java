package customer.batchimportcat.service.cqn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.sap.cds.Result;
import com.sap.cds.Row;
import com.sap.cds.ql.Select;

import cds.gen.dataimportservice.BatchImportConfig;
import cds.gen.dataimportservice.BatchImportStructure;
import cds.gen.dataimportservice.BatchImportStructure_;
import cds.gen.dataimportservice.DataImportService;
import cds.gen.dataimportservice.DataImportService_;

@Service
public class BatchImportStructureCqnService {
    // private final CqnService dataImportService;
    private final DataImportService dataImportService;

    public BatchImportStructureCqnService(
            @Qualifier(DataImportService_.CDS_NAME) DataImportService dataImportService) {
        this.dataImportService = dataImportService;
    }

    public void validateSingleRootStructure(BatchImportStructure structure) {
        if (structure == null || !Boolean.TRUE.equals(structure.getRootNode())) {
            return;
        }

        boolean isActiveEntity = !Boolean.FALSE.equals(structure.getIsActiveEntity());
        String configUUID = resolveConfigUUID(structure, isActiveEntity);
        if (isBlank(configUUID)) {
            return;
        }

        List<String> rootStructureIds = findRootStructureIds(configUUID, isActiveEntity);
        if (rootStructureIds.isEmpty()) {
            return;
        }

        String currentStructureId = structure.getId();
        for (String rootStructureId : rootStructureIds) {
            if (isBlank(currentStructureId) || !currentStructureId.equals(rootStructureId)) {
                throw new IllegalStateException(
                        "Only one root sheet is allowed for configuration " + configUUID + ".");
            }
        }
    }

    public void ensureSingleRootStructure(String configUUID, boolean isActiveEntity) {
        List<String> rootStructureIds = findRootStructureIds(configUUID, isActiveEntity);
        if (rootStructureIds.size() > 1) {
            throw new IllegalStateException(
                    "Only one root sheet is allowed for configuration " + configUUID + ".");
        }
    }

    public List<Map<String, Serializable>> loadSerializableByConfigUUID(String configUUID) {
        Result result = dataImportService.run(
                Select.from(BatchImportStructure_.class)
                        .where(structure -> structure.ConfigUUID().eq(configUUID)));
        return toSerializableRows(result);
    }

    private String resolveConfigUUID(BatchImportStructure structure, boolean isActiveEntity) {
        if (!isBlank(structure.getConfigUUID())) {
            return structure.getConfigUUID();
        }

        BatchImportConfig toConfig = structure.getToConfig();
        if (toConfig != null && !isBlank(toConfig.getId())) {
            return toConfig.getId();
        }

        if (isBlank(structure.getId())) {
            return null;
        }

        Result existingResult = dataImportService.run(
                Select.from(cds.gen.dataimportservice.BatchImportStructure_.class)
                        .columns(row -> row.ConfigUUID())
                        .where(row -> row.ID().eq(structure.getId())
                                .and(row.IsActiveEntity().eq(isActiveEntity))));
        // return existingResult.first().map(row -> stringValue(row.get("ConfigUUID"))).orElse(null);
        return existingResult.first(BatchImportStructure.class).orElse(null).getConfigUUID();
    }

    private List<String> findRootStructureIds(String configUUID, boolean isActiveEntity) {
        Result rootStructuresResult = dataImportService.run(
                Select.from(cds.gen.dataimportservice.BatchImportStructure_.class)
                        .columns(row -> row.ID())
                        .where(row -> row.ConfigUUID().eq(configUUID)
                                .and(row.RootNode().eq(true))
                                .and(row.IsActiveEntity().eq(isActiveEntity))));

        List<String> rootStructureIds = new ArrayList<>();
        for (BatchImportStructure row : rootStructuresResult.listOf(BatchImportStructure.class)) {
            String structureId = row.getId();
            if (structureId != null) {
                rootStructureIds.add(structureId);
            }
        }
        return rootStructureIds;
    }

    private List<Map<String, Serializable>> toSerializableRows(Result result) {
        List<Map<String, Serializable>> rows = new ArrayList<>();
        for (Row row : result.listOf(Row.class)) {
            rows.add(toSerializableMap(row));
        }
        return rows;
    }

    private Map<String, Serializable> toSerializableMap(Row row) {
        Map<String, Serializable> serializableData = new HashMap<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getValue() instanceof Serializable serializable) {
                serializableData.put(entry.getKey(), serializable);
            }
        }
        return serializableData;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
