package customer.batchimportcat.service.cqn;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sap.cds.Result;
import com.sap.cds.Row;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.CqnUpdate;
import com.sap.cds.services.persistence.PersistenceService;

import cds.gen.dataimportservice.BatchImportConfig_;
import cds.gen.dataimportservice.DataImportService;
// import cds.gen.zzdt.BatchImportConfig_;
import customer.batchimportcat.model.BatchImportConfigLaunchContext;

@Service
public class BatchImportConfigCqnService {
    private final DataImportService dataImportService;
    private final PersistenceService db;

    public BatchImportConfigCqnService( DataImportService dataImportService, PersistenceService db) {
        this.dataImportService = dataImportService;
        this.db = db;
    }

    public BatchImportConfigLaunchContext loadRequiredLaunchContext(String configUUID) {
        Result configResult = dataImportService
                .run(Select.from(BatchImportConfig_.class).where(config -> config.ID().eq(configUUID)));
        if (configResult.rowCount() <= 0) {
            throw new IllegalStateException("Batch import config " + configUUID + " was not found.");
        }
        Row configRow = configResult.first().orElseThrow();
        String processKey = stringValue(configRow.get("ProcessKey"));
        if (processKey == null || processKey.isBlank()) {
            throw new IllegalStateException("Batch import config " + configUUID + " is missing ProcessKey.");
        }

        return new BatchImportConfigLaunchContext(
                configUUID,
                processKey,
                stringValue(configRow.get("Object")),
                stringValue(configRow.get("ObjectName")));
    }

    public Map<String, Serializable> loadRequiredSerializable(String configUUID) {
        Result configResult = dataImportService
                .run(Select.from(BatchImportConfig_.class).where(config -> config.ID().eq(configUUID)));
        if (configResult.rowCount() <= 0) {
            throw new IllegalStateException("Batch import config " + configUUID + " was not found.");
        }
        return toSerializableMap(configResult.first().orElseThrow());
    }

    public void updateTemplateWithDb(String configUUID, byte[] template, String mimeType, String fileName) {
        Map<String, Object> data = new HashMap<>();
        data.put("Template", template);
        data.put("MimeType", mimeType);
        data.put("FileName", fileName);

        CqnUpdate update = Update.entity(BatchImportConfig_.class)
                .data(data)
                .where(row -> row.ID().eq(configUUID));
        db.run(update);
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

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
