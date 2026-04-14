package customer.batchimportcat.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.CqnUpdate;
import com.sap.cds.services.cds.CqnService;

import cds.gen.dataimportservice.BatchImportFile_;
import cds.gen.dataimportservice.DataImportService_;

@Component
public class BatchImportFileStatusService {
    private final CqnService dataImportService;

    public BatchImportFileStatusService(@Qualifier(DataImportService_.CDS_NAME) CqnService dataImportService) {
        this.dataImportService = dataImportService;
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
}
