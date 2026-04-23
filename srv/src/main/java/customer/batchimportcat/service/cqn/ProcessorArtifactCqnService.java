package customer.batchimportcat.service.cqn;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.sap.cds.Result;
import com.sap.cds.ql.Select;

import cds.gen.dataimportservice.DataImportService;
import cds.gen.dataimportservice.ProcessorArtifact;
import cds.gen.dataimportservice.ProcessorArtifact_;
// import cds.gen.zzdt.ProcessorArtifact_;
import customer.batchimportcat.model.ProcessorArtifactBinding;

@Service
public class ProcessorArtifactCqnService {
    private final DataImportService dataImportService;

    public ProcessorArtifactCqnService(DataImportService dataImportService) {
        this.dataImportService = dataImportService;
    }

    public void validateEnabledArtifact(String artifactId, String processKey) {
        Result result = dataImportService.run(Select.from(ProcessorArtifact_.class)
                .columns(row -> row.ID())
                .where(row -> row.ProcessKey().eq(processKey).and(row.Enabled().eq(true))));
        for (ProcessorArtifact row : result.listOf(ProcessorArtifact.class)) {
            String existingId = row.getId();
            if (artifactId == null || !artifactId.equals(existingId)) {
                throw new IllegalStateException(
                        "Only one enabled ProcessorArtifact is allowed for process key " + processKey + ".");
            }
        }
    }

    public void updateMetadata(ProcessorArtifact artifact, String checksum, long mediaSize
    // String mimeType
    ) {
        if (artifact == null) {
            return;
        }
        artifact.setArtifactChecksum(checksum);
        artifact.setMediaSize(mediaSize);
        // data.put("MediaMimeType", mimeType); -- IGNORE -- The MIME type is determined
        // by the CAP standard CQN service, so it should not be updated here.
    }

    public ProcessorArtifactBinding getRequiredEnabledArtifact(String processKey) {
        Result result = dataImportService.run(Select.from(ProcessorArtifact_.class)
                .where(artifact -> artifact.ProcessKey().eq(processKey).and(artifact.Enabled().eq(true))));
        if (result.rowCount() <= 0) {
            throw new IllegalStateException("No enabled processor artifact is maintained for process key " + processKey
                    + ".");
        }
        // Row row = result.first().orElseThrow();
        ProcessorArtifact row = result.first(ProcessorArtifact.class).orElseThrow();
        return new ProcessorArtifactBinding(
                String.valueOf(row.getId()),
                row.getProcessKey(),
                row.getVersion(),
                row.getDescription(),
                row.getArtifactChecksum(),
            row.getEntryPointClass());
    }

    public List<Map<String, Object>> getValueHelps() {
        Result result = dataImportService.run(Select.from(ProcessorArtifact_.class)
                .where(artifact -> artifact.Enabled().eq(true)));
        Map<String, Map<String, Object>> rowsByProcessKey = new LinkedHashMap<>();
        // for (Row row : result.listOf(Row.class)) {
        for (ProcessorArtifact row : result.listOf(ProcessorArtifact.class)) {
            String processKey = row.getProcessKey();
            if (processKey == null || processKey.isBlank()) {
                continue;
            }
            rowsByProcessKey.computeIfAbsent(processKey, ignored -> {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("ProcessKey", processKey);
                entry.put("Description", Objects.requireNonNullElse(row.getDescription(), processKey));
                return entry;
            });
        }
        return new ArrayList<>(rowsByProcessKey.values());
    }

    public List<Map<String, Object>> getImplementedClasses() {
        Result result = dataImportService.run(Select.from(ProcessorArtifact_.class)
                .where(artifact -> artifact.Enabled().eq(true)));
        Map<String, Map<String, Object>> rowsByEntryPoint = new LinkedHashMap<>();
        for (ProcessorArtifact row : result.listOf(ProcessorArtifact.class)) {
            String entryPointClass = row.getEntryPointClass();
            if (entryPointClass == null || entryPointClass.isBlank()) {
                continue;
            }
            rowsByEntryPoint.computeIfAbsent(entryPointClass, ignored -> {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("Name", entryPointClass);
                entry.put("Description",
                        Objects.requireNonNullElse(row.getDescription(), entryPointClass));
                return entry;
            });
        }
        return new ArrayList<>(rowsByEntryPoint.values());
    }

}
