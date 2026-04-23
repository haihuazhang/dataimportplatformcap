package customer.batchimportcat.handlers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

import cds.gen.dataimportservice.DataImportService_;
import cds.gen.dataimportservice.ProcessorArtifact;
import cds.gen.dataimportservice.ProcessorArtifact_;
import customer.batchimportcat.service.cqn.ProcessorArtifactCqnService;

@Component
@ServiceName(DataImportService_.CDS_NAME)
public class ProcessorArtifactHandler implements EventHandler {
    private final ProcessorArtifactCqnService processorArtifactCqnService;

    public ProcessorArtifactHandler(ProcessorArtifactCqnService processorArtifactCqnService) {
        this.processorArtifactCqnService = processorArtifactCqnService;
    }

    @Before(event = { "CREATE", "UPDATE" }, entity = ProcessorArtifact_.CDS_NAME)
    public void validateEnabledArtifact(Stream<ProcessorArtifact> artifacts) {
        artifacts.forEach(artifact -> {
            if (artifact == null) {
                return;
            }
            if (artifact.getArtifactStorageType() == null || artifact.getArtifactStorageType().isBlank()) {
                artifact.setArtifactStorageType("DB_MEDIA");
            }
            if (!Boolean.TRUE.equals(artifact.getEnabled())) {
                return;
            }
            if (isBlank(artifact.getProcessKey())) {
                throw new IllegalStateException("ProcessKey is required for an enabled ProcessorArtifact.");
            }
            if (isBlank(artifact.getVersion())) {
                throw new IllegalStateException("Version is required for an enabled ProcessorArtifact.");
            }
            if (isBlank(artifact.getEntryPointClass())) {
                throw new IllegalStateException("EntryPointClass is required for an enabled ProcessorArtifact.");
            }

            processorArtifactCqnService.validateEnabledArtifact(artifact.getId(), artifact.getProcessKey());
        });
    }

    @On(event = { CqnService.EVENT_UPDATE, CqnService.EVENT_CREATE }, entity = ProcessorArtifact_.CDS_NAME)
    public void refreshArtifactMetadata(Stream<ProcessorArtifact> artifacts) {
        artifacts.forEach(artifact -> {
            if (artifact == null) {
                return;
            }
            byte[] mediaContent = readMediaContentFromPayload(artifact);
            if (mediaContent == null || mediaContent.length == 0) {
                return;
            }
            artifact.setMediaContent(new ByteArrayInputStream(mediaContent));
            processorArtifactCqnService.updateMetadata(
                    artifact,
                    sha256(mediaContent),
                    (long) mediaContent.length
            // storedContent.mimeType() -- IGNORE -- The MIME type is determined by the CAP
            // standard CQN service, so it should not be updated here.
            );
        });
    }

    private byte[] readMediaContentFromPayload(ProcessorArtifact artifact) {
        try (InputStream inputStream = artifact.getMediaContent()) {
            if (inputStream == null) {
                return null;
            }
            return inputStream.readAllBytes();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to read processor artifact media content from request payload.",
                    exception);
        }
    }

    private String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] checksum = digest.digest(content);
            StringBuilder builder = new StringBuilder(checksum.length * 2);
            for (byte value : checksum) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to calculate artifact checksum.", exception);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
