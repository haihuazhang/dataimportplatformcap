package customer.batchimportcat.service.cqn.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sap.cds.Result;
import com.sap.cds.Row;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Insert;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.CqnInsert;
import com.sap.cds.ql.cqn.CqnUpdate;
import com.sap.cds.services.persistence.PersistenceService;
import com.sap.cds.services.runtime.CdsRuntime;

import customer.batchimportcat.batch.dynamic.dto.BatchImportConfigData;
import customer.batchimportcat.batch.dynamic.types.BatchImportOriginalDataRecord;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessMessage;
import customer.batchimportcat.service.cqn.BatchImportPersistenceService;

@Service
public class BatchImportDBCqnService implements BatchImportPersistenceService {
    private static final String CONFIG_ENTITY = "zzdt.BatchImportConfig";
    private static final String STRUCTURE_ENTITY = "zzdt.BatchImportStructure";
    private static final String FIELD_ENTITY = "zzdt.BatchImportField";
    private static final String FILE_ENTITY = "zzdt.BatchImportFile";
    private static final String DATA_ENTITY = "zzdt.BatchImportData";
    private static final String MESSAGE_ENTITY = "zzdt.BatchImportMessage";

    private final PersistenceService db;
    private final CdsRuntime runtime;

    public BatchImportDBCqnService(PersistenceService db, CdsRuntime runtime) {
        this.db = db;
        this.runtime = runtime;
    }

    @Override
    public byte[] loadFileContent(String fileUUID) {
        return CdsChangeSetExecutor.runTransactional(runtime, () -> {
            Row fileRow = readFileContentRow(fileUUID);
            return readBinaryContent(fileRow, "Attachment",
                    "Failed to read file content for " + fileUUID + ".");
        });
    }

    @Override
    public BatchImportConfigData loadConfigData(String fileUUID) {
        return CdsChangeSetExecutor.runTransactional(runtime,
                () -> loadConfigDataInChangeSet(fileUUID));
    }

    private BatchImportConfigData loadConfigDataInChangeSet(String fileUUID) {
        Row fileRow = readFileConfigRow(fileUUID);
        String configUUID = String.valueOf(fileRow.get("ConfigUUID"));

        Result configResult = db.run(Select.from(CONFIG_ENTITY).byId(configUUID));
        if (configResult.rowCount() <= 0) {
            throw new IllegalStateException("Batch import config " + configUUID + " was not found.");
        }

        Map<String, Serializable> configData = toSerializableMap(configResult.first().orElseThrow());
        List<Map<String, Serializable>> structureRows = toSerializableRows(db.run(
                Select.from(STRUCTURE_ENTITY)
                        .where(CQL.get("ConfigUUID").eq(configUUID))));
        List<Map<String, Serializable>> fieldRows = toSerializableRows(db.run(
                Select.from(FIELD_ENTITY)
                        .where(CQL.get("ConfigUUID").eq(configUUID))));

        return new BatchImportConfigData(configData, structureRows, fieldRows);
    }

    @Override
    public void saveOriginalData(String fileUUID, List<BatchImportOriginalDataRecord> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }

        List<Map<String, Object>> dataEntries = new ArrayList<>(entries.size());
        for (BatchImportOriginalDataRecord entry : entries) {
            Map<String, Object> data = new HashMap<>();
            data.put("FileUUID", fileUUID);
            data.put("Line", entry.line());
            data.put("StructureName", entry.structureName());
            data.put("DataJson", entry.dataJson());
            dataEntries.add(data);
        }

        CqnInsert insert = Insert.into(DATA_ENTITY).entries(dataEntries);
        db.run(insert);
    }

    @Override
    public void saveMessages(String fileUUID, List<BatchImportProcessMessage> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }

        List<Map<String, Object>> messageEntries = new ArrayList<>(entries.size());
        for (BatchImportProcessMessage entry : entries) {
            Map<String, Object> message = new HashMap<>();
            message.put("FileUUID", fileUUID);
            message.put("Line", entry.line());
            message.put("Type", entry.type());
            message.put("Code", normalizeMessageCode(entry.code()));
            message.put("Message", entry.message());
            message.put("Details", entry.details());
            messageEntries.add(message);
        }

        CqnInsert insert = Insert.into(MESSAGE_ENTITY).entries(messageEntries);
        db.run(insert);
    }

    @Override
    public void updateFileExecutionInfo(String fileUUID, Long jobInstanceId, String status, String statusText,
            int criticality) {
        Map<String, Object> data = new HashMap<>();
        if (jobInstanceId != null) {
            data.put("JobName", String.valueOf(jobInstanceId));
        }
        data.put("Status", status);
        data.put("StatusText", statusText);
        data.put("StatusCriticality", criticality);

        CqnUpdate update = Update.entity(FILE_ENTITY)
                .data(data)
                .byId(fileUUID);
        db.run(update);
    }

    private Row readFileContentRow(String fileUUID) {
        return readFileRow(Select.from(FILE_ENTITY).columns("Attachment").byId(fileUUID), fileUUID);
    }

    private Row readFileConfigRow(String fileUUID) {
        return readFileRow(Select.from(FILE_ENTITY).columns("ConfigUUID").byId(fileUUID), fileUUID);
    }

    private Row readFileRow(Select<?> select, String fileUUID) {
        Result fileResult = db.run(select);
        if (fileResult.rowCount() <= 0) {
            throw new IllegalStateException("Batch import file " + fileUUID + " was not found.");
        }
        return fileResult.first().orElseThrow();
    }

    private String normalizeMessageCode(String code) {
        if (code == null || code.isBlank()) {
            return code;
        }
        String normalized = code.trim().toLowerCase();
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private byte[] readBinaryContent(Row row, String key, String errorMessage) {
        return BinaryContentReader.readRequired(row.get(key), errorMessage);
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
}
