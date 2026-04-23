package customer.batchimportcat.service.cqn;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sap.cds.Result;
// import com.sap.cds.Row;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.CqnUpdate;

import cds.gen.dataimportservice.BatchImportFile;
import cds.gen.dataimportservice.BatchImportFile_;
import cds.gen.dataimportservice.DataImportService;
// import cds.gen.zzdt.BatchImportFile_;

@Service
public class BatchImportFileCqnService {
    private final DataImportService dataImportService;

    public BatchImportFileCqnService( DataImportService dataImportService) {
        this.dataImportService = dataImportService;
    }

    public String loadRequiredConfigUUID(String fileUUID) {
        Result fileResult = dataImportService
                .run(Select.from(BatchImportFile_.class).where(file -> file.ID().eq(fileUUID)));
        if (fileResult.rowCount() <= 0) {
            throw new IllegalStateException("Batch import file " + fileUUID + " was not found.");
        }
        BatchImportFile fileRow = fileResult.single(BatchImportFile.class);
        String configUUID = fileRow.getConfigUUID();
        if (configUUID == null || configUUID.isBlank()) {
            throw new IllegalStateException("Batch import file " + fileUUID + " is missing ConfigUUID.");
        }
        return configUUID;
    }

    public void markQueued(String fileUUID, Long jobInstanceId) {
        updateFileExecutionInfo(fileUUID, jobInstanceId, "Q", "Queued", 2);
    }

    public void markRunning(String fileUUID, Long jobInstanceId) {
        updateFileExecutionInfo(fileUUID, jobInstanceId, "R", "Running", 2);
    }

    public void markSuccess(String fileUUID, Long jobInstanceId) {
        updateFileExecutionInfo(fileUUID, jobInstanceId, "S", "Success", 3);
    }

    public void markError(String fileUUID, Long jobInstanceId) {
        updateFileExecutionInfo(fileUUID, jobInstanceId, "E", "Error", 1);
    }

    private void updateFileExecutionInfo(String fileUUID, Long jobInstanceId, String status, String statusText,
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
        dataImportService.run(update);
    }

    // private String stringValue(Object value) {
    //     return value == null ? null : String.valueOf(value);
    // }
}
