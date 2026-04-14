package customer.batchimportcat.service;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.sap.cds.Result;
import com.sap.cds.Row;
import com.sap.cds.ql.Insert;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.CqnInsert;
import com.sap.cds.ql.cqn.CqnUpdate;
import com.sap.cds.services.cds.CqnService;

import cds.gen.dataimportservice.BatchImportConfig_;
import cds.gen.dataimportservice.BatchImportData;
import cds.gen.dataimportservice.BatchImportData_;
import cds.gen.dataimportservice.BatchImportField_;
import cds.gen.dataimportservice.BatchImportFile_;
import cds.gen.dataimportservice.BatchImportMessage;
import cds.gen.dataimportservice.BatchImportMessage_;
import cds.gen.dataimportservice.BatchImportStructure_;
import cds.gen.dataimportservice.DataImportService_;
import customer.batchimportcat.batch.dynamic.dto.BatchImportConfigData;
import customer.batchimportcat.batch.dynamic.types.BatchImportOriginalDataRecord;
import customer.batchimportcat.batch.dynamic.types.BatchImportProcessMessage;
import customer.batchimportcat.batch.exceptions.BatchExceptionsUtil;

@Service
public class CdsBatchImportPersistenceService implements BatchImportPersistenceService {
    private final CqnService dataImportService;

    public CdsBatchImportPersistenceService(@Qualifier(DataImportService_.CDS_NAME) CqnService dataImportService) {
        this.dataImportService = dataImportService;
    }

    @Override
    public byte[] loadFileContent(String fileUUID) {
        Row fileRow = readFileRow(fileUUID);
        return readFileContent(fileRow);
    }

    @Override
    public BatchImportConfigData loadConfigData(String fileUUID) {
        Row fileRow = readFileRow(fileUUID);
        String configUUID = String.valueOf(fileRow.get("ConfigUUID"));

        Result configResult = dataImportService.run(
                Select.from(BatchImportConfig_.class).where(config -> config.ID().eq(configUUID)));
        if (configResult.rowCount() <= 0) {
            throw BatchExceptionsUtil.geBatchConfigNotFound(configUUID);
        }

        Map<String, Serializable> configData = toSerializableMap(configResult.first().orElseThrow());
        List<Map<String, Serializable>> structureRows = toSerializableRows(dataImportService.run(
                Select.from(BatchImportStructure_.class)
                        .where(structure -> structure.ConfigUUID().eq(configUUID))));
        List<Map<String, Serializable>> fieldRows = toSerializableRows(dataImportService.run(
                Select.from(BatchImportField_.class)
                        .where(field -> field.ConfigUUID().eq(configUUID))));

        return new BatchImportConfigData(configData, structureRows, fieldRows);
    }

    private Row readFileRow(String fileUUID) {
        Result fileResult = dataImportService
                .run(Select.from(BatchImportFile_.class).where(file -> file.ID().eq(fileUUID)));
        if (fileResult.rowCount() <= 0) {
            throw BatchExceptionsUtil.getBatchFileNotFound(fileUUID);
        }

        return fileResult.first().orElseThrow(() -> BatchExceptionsUtil.getBatchFileNotFound(fileUUID));
    }

    @Override
    public void saveOriginalData(String fileUUID, List<BatchImportOriginalDataRecord> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }

        List<BatchImportData> dataEntries = new ArrayList<>(entries.size());
        for (BatchImportOriginalDataRecord entry : entries) {
            BatchImportData data = BatchImportData.create();
            data.setFileUUID(fileUUID);
            data.setLine(entry.line());
            data.setStructureName(entry.structureName());
            data.setDataJson(entry.dataJson());
            dataEntries.add(data);
        }

        CqnInsert insert = Insert.into(BatchImportData_.class).entries(dataEntries);
        dataImportService.run(insert);
    }

    @Override
    public void saveMessages(String fileUUID, List<BatchImportProcessMessage> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }

        List<BatchImportMessage> messageEntries = new ArrayList<>(entries.size());
        for (BatchImportProcessMessage entry : entries) {
            BatchImportMessage message = BatchImportMessage.create();
            message.setFileUUID(fileUUID);
            message.setLine(entry.line());
            message.setType(entry.type());
            message.setCode(entry.code());
            message.setMessage(entry.message());
            message.setDetails(entry.details());
            messageEntries.add(message);
        }

        CqnInsert insert = Insert.into(BatchImportMessage_.class).entries(messageEntries);
        dataImportService.run(insert);
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

        CqnUpdate update = Update.entity(BatchImportFile_.class)
                .data(data)
                .where(file -> file.ID().eq(fileUUID));
        Result result = dataImportService.run(update);
    }

    private byte[] readFileContent(Row fileRow) {
        try (InputStream inputStream = (InputStream) fileRow.get("Attachment")) {
            if (inputStream == null) {
                throw BatchExceptionsUtil.getBatchFileNotFound(String.valueOf(fileRow.get("ID")));
            }
            return inputStream.readAllBytes();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to read file content.", exception);
        }
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
