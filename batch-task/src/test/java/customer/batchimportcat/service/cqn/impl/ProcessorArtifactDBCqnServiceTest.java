package customer.batchimportcat.service.cqn.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.sap.cds.Result;
import com.sap.cds.Row;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.services.changeset.ChangeSetContext;
import com.sap.cds.services.persistence.PersistenceService;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.runtime.ChangeSetContextRunner;

import customer.batchimportcat.batchtask.runtime.ResolvedProcessorArtifact;

class ProcessorArtifactDBCqnServiceTest {
    @Test
    void resolveReadsProcessorMediaContentInsideCapChangeSet() {
        byte[] content = new byte[] { 4, 5, 6 };
        PersistenceService db = mock(PersistenceService.class);
        CdsRuntime runtime = mock(CdsRuntime.class);
        ChangeSetContextRunner runner = changeSetRunner();
        when(runtime.changeSetContext()).thenReturn(runner);
        Result executionResult = result(executionRow());
        Row artifactRow = artifactRow();
        Result artifactResult = result(artifactRow);
        AtomicBoolean insideChangeSet = new AtomicBoolean(false);
        Result mediaResult = result(mediaRow(assertingStream(content, insideChangeSet)));
        when(runner.run(anyResolveAction())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Function<ChangeSetContext, ResolvedProcessorArtifact> action = invocation.getArgument(0);
            insideChangeSet.set(true);
            try {
                return action.apply(mock(ChangeSetContext.class));
            } finally {
                insideChangeSet.set(false);
            }
        });
        when(db.run(any(CqnSelect.class))).thenReturn(executionResult, artifactResult, mediaResult);
        ProcessorArtifactDBCqnService service = new ProcessorArtifactDBCqnService(db, runtime);

        ResolvedProcessorArtifact artifact = service.resolve("execution-1");

        assertEquals("artifact-1", artifact.processorArtifactId());
        assertEquals("NOOP", artifact.processKey());
        assertArrayEquals(content, artifact.mediaContent());
        verify(runner).markTransactional();
        verify(artifactRow, never()).get("MediaContent");
    }

    private static Result result(Row row) {
        Result result = mock(Result.class);
        when(result.rowCount()).thenReturn(1L);
        when(result.first()).thenReturn(Optional.of(row));
        return result;
    }

    private static Row executionRow() {
        Row row = mock(Row.class);
        when(row.get("ProcessorArtifactID")).thenReturn("artifact-1");
        when(row.get("ProcessKey")).thenReturn("NOOP");
        when(row.get("ProcessorVersion")).thenReturn("1.0.0");
        when(row.get("ArtifactChecksum")).thenReturn("checksum");
        return row;
    }

    private static Row artifactRow() {
        Row row = mock(Row.class);
        when(row.get("ArtifactStorageType")).thenReturn("DB_MEDIA");
        when(row.get("EntryPointClass"))
                .thenReturn("customer.batchimportcat.processor.noop.NoopBatchImportProcessor");
        when(row.get("MediaUrl")).thenReturn(null);
        return row;
    }

    private static Row mediaRow(Object mediaContent) {
        Row row = mock(Row.class);
        when(row.get("MediaContent")).thenReturn(mediaContent);
        return row;
    }

    private static ChangeSetContextRunner changeSetRunner() {
        ChangeSetContextRunner runner = mock(ChangeSetContextRunner.class);
        when(runner.markTransactional()).thenReturn(runner);
        return runner;
    }

    @SuppressWarnings("unchecked")
    private static Function<ChangeSetContext, ResolvedProcessorArtifact> anyResolveAction() {
        return any(Function.class);
    }

    private static FilterInputStream assertingStream(byte[] content, AtomicBoolean insideChangeSet) {
        return new FilterInputStream(new ByteArrayInputStream(content)) {
            @Override
            public int read(byte[] buffer, int offset, int length) throws IOException {
                assertTrue(insideChangeSet.get(), "Media stream must be consumed before CAP ChangeSet closes.");
                return super.read(buffer, offset, length);
            }
        };
    }
}
