package customer.batchimportcat.service.cqn.impl;

import org.springframework.stereotype.Service;

import com.sap.cds.Result;
import com.sap.cds.Row;
import com.sap.cds.ql.Select;
import com.sap.cds.services.persistence.PersistenceService;
import com.sap.cds.services.runtime.CdsRuntime;

import customer.batchimportcat.batchtask.runtime.ResolvedProcessorArtifact;
import customer.batchimportcat.service.cqn.ProcessorArtifactResolver;

@Service
public class ProcessorArtifactDBCqnService implements ProcessorArtifactResolver {
    private static final String EXECUTION_ENTITY = "zzdt.BatchImportExecution";
    private static final String ARTIFACT_ENTITY = "zzdt.ProcessorArtifact";

    private final PersistenceService db;
    private final CdsRuntime runtime;

    public ProcessorArtifactDBCqnService(PersistenceService db, CdsRuntime runtime) {
        this.db = db;
        this.runtime = runtime;
    }

    @Override
    public ResolvedProcessorArtifact resolve(String executionUUID) {
        return CdsChangeSetExecutor.runTransactional(runtime,
                () -> resolveInChangeSet(executionUUID));
    }

    private ResolvedProcessorArtifact resolveInChangeSet(String executionUUID) {
        Row executionRow = loadExecutionRow(executionUUID);
        String processorArtifactId = String.valueOf(executionRow.get("ProcessorArtifactID"));
        Row artifactRow = loadArtifactRow(processorArtifactId);
        byte[] mediaContent = loadMediaContent(processorArtifactId);

        return new ResolvedProcessorArtifact(
                executionUUID,
                processorArtifactId,
                String.valueOf(executionRow.get("ProcessKey")),
                String.valueOf(executionRow.get("ProcessorVersion")),
                stringValue(executionRow.get("ArtifactChecksum")),
                stringValue(artifactRow.get("ArtifactStorageType")),
                stringValue(artifactRow.get("EntryPointClass")),
                stringValue(artifactRow.get("MediaUrl")),
                mediaContent);
    }

    private Row loadExecutionRow(String executionUUID) {
        Result result = db.run(Select.from(EXECUTION_ENTITY).byId(executionUUID));
        if (result.rowCount() <= 0) {
            throw new IllegalStateException("Batch import execution " + executionUUID + " was not found.");
        }
        return result.first().orElseThrow();
    }

    private Row loadArtifactRow(String processorArtifactId) {
        Result result = db.run(Select.from(ARTIFACT_ENTITY)
                .columns("ArtifactStorageType", "EntryPointClass", "MediaUrl")
                .byId(processorArtifactId));
        if (result.rowCount() <= 0) {
            throw new IllegalStateException("Processor artifact " + processorArtifactId + " was not found.");
        }
        return result.first().orElseThrow();
    }

    private byte[] loadMediaContent(String processorArtifactId) {
        Result result = db.run(Select.from(ARTIFACT_ENTITY)
                .columns("MediaContent")
                .byId(processorArtifactId));
        Row mediaRow = result.first().orElse(null);
        if (mediaRow == null) {
            return null;
        }
        return BinaryContentReader.readNullable(mediaRow.get("MediaContent"),
                "Failed to read processor artifact media content.");
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
