package customer.batchimportcat.service.cqn.impl;

import java.io.InputStream;

import org.springframework.stereotype.Service;

import com.sap.cds.Result;
import com.sap.cds.Row;
import com.sap.cds.ql.Select;
import com.sap.cds.services.persistence.PersistenceService;

import customer.batchimportcat.batchtask.runtime.ResolvedProcessorArtifact;
import customer.batchimportcat.service.cqn.ProcessorArtifactResolver;

@Service
public class ProcessorArtifactDBCqnService implements ProcessorArtifactResolver {
    private static final String EXECUTION_ENTITY = "zzdt.BatchImportExecution";
    private static final String ARTIFACT_ENTITY = "zzdt.ProcessorArtifact";

    private final PersistenceService db;

    public ProcessorArtifactDBCqnService(PersistenceService db) {
        this.db = db;
    }

    @Override
    public ResolvedProcessorArtifact resolve(String executionUUID) {
        Row executionRow = loadExecutionRow(executionUUID);
        String processorArtifactId = String.valueOf(executionRow.get("ProcessorArtifactID"));
        Row artifactRow = loadArtifactRow(processorArtifactId);

        return new ResolvedProcessorArtifact(
                executionUUID,
                processorArtifactId,
                String.valueOf(executionRow.get("ProcessKey")),
                String.valueOf(executionRow.get("ProcessorVersion")),
                stringValue(executionRow.get("ArtifactChecksum")),
                stringValue(artifactRow.get("ArtifactStorageType")),
                stringValue(artifactRow.get("EntryPointClass")),
                stringValue(artifactRow.get("MediaUrl")),
                readMediaContent(artifactRow));
    }

    private Row loadExecutionRow(String executionUUID) {
        Result result = db.run(Select.from(EXECUTION_ENTITY).byId(executionUUID));
        if (result.rowCount() <= 0) {
            throw new IllegalStateException("Batch import execution " + executionUUID + " was not found.");
        }
        return result.first().orElseThrow();
    }

    private Row loadArtifactRow(String processorArtifactId) {
        Result result = db.run(Select.from(ARTIFACT_ENTITY).byId(processorArtifactId));
        if (result.rowCount() <= 0) {
            throw new IllegalStateException("Processor artifact " + processorArtifactId + " was not found.");
        }
        return result.first().orElseThrow();
    }

    private byte[] readMediaContent(Row artifactRow) {
        try (InputStream inputStream = (InputStream) artifactRow.get("MediaContent")) {
            if (inputStream == null) {
                return null;
            }
            return inputStream.readAllBytes();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to read processor artifact media content.", exception);
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
