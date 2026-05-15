package customer.batchimportcat.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.sap.cds.ql.cqn.CqnInsert;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.changeset.ChangeSetContext;
import com.sap.cds.services.changeset.ChangeSetListener;
import com.sap.cds.services.draft.DraftService;

import cds.gen.dataimportservice.BatchImportFile;
import customer.batchimportcat.model.BatchImportExecutionRef;
import customer.batchimportcat.service.BatchImportTaskTriggerService;

class DataImportServiceHandlerTest {

    @Test
    void createWithoutDraftActivateHintDoesNotRegisterTrigger() {
        RecordingBatchImportTaskTriggerService triggerService = new RecordingBatchImportTaskTriggerService();
        DataImportServiceHandler handler = new DataImportServiceHandler(null, null, triggerService);
        RecordingChangeSetContext changeSetContext = new RecordingChangeSetContext();

        CdsCreateEventContext context = createContext(Map.of(), changeSetContext);

        handler.callBatchJobAfterCreate(Stream.of(batchImportFile("file-1", null)), context);

        assertEquals(0, changeSetContext.registeredListeners.size());
        assertEquals(List.of(), triggerService.triggeredFileUUIDs);
    }

    @Test
    void draftActivateCreateTriggersAfterSuccessfulChangeSetClose() {
        RecordingBatchImportTaskTriggerService triggerService = new RecordingBatchImportTaskTriggerService();
        DataImportServiceHandler handler = new DataImportServiceHandler(null, null, triggerService);
        RecordingChangeSetContext changeSetContext = new RecordingChangeSetContext();

        CdsCreateEventContext context = createContext(Map.of(DraftService.EVENT_DRAFT_SAVE, true), changeSetContext);

        handler.callBatchJobAfterCreate(Stream.of(batchImportFile("file-1", null)), context);

        assertEquals(1, changeSetContext.registeredListeners.size());
        assertEquals(List.of(), triggerService.triggeredFileUUIDs);

        changeSetContext.registeredListeners.get(0).afterClose(true);

        assertEquals(List.of("file-1"), triggerService.triggeredFileUUIDs);
    }


    private static CdsCreateEventContext createContext(Map<String, Object> hints, ChangeSetContext changeSetContext) {
        CqnInsert cqn = mock(CqnInsert.class);
        when(cqn.hints()).thenReturn(hints);

        CdsCreateEventContext context = mock(CdsCreateEventContext.class);
        when(context.getCqn()).thenReturn(cqn);
        when(context.getChangeSetContext()).thenReturn(changeSetContext);
        return context;
    }


    private static BatchImportFile batchImportFile(String id, Boolean isActiveEntity) {
        BatchImportFile file = BatchImportFile.create();
        file.setId(id);
        file.setIsActiveEntity(isActiveEntity);
        return file;
    }

    private static final class RecordingChangeSetContext implements ChangeSetContext {
        private final List<ChangeSetListener> registeredListeners = new ArrayList<>();

        @Override
        public int getId() {
            return 1;
        }

        @Override
        public void markForCancel() {
        }

        @Override
        public boolean isMarkedForCancel() {
            return false;
        }

        @Override
        public void markTransactional() {
        }

        @Override
        public boolean isMarkedTransactional() {
            return false;
        }

        @Override
        public void register(ChangeSetListener listener) {
            registeredListeners.add(listener);
        }
    }

    private static final class RecordingBatchImportTaskTriggerService implements BatchImportTaskTriggerService {
        private final List<String> triggeredFileUUIDs = new ArrayList<>();

        @Override
        public BatchImportExecutionRef trigger(String fileUUID) {
            triggeredFileUUIDs.add(fileUUID);
            return null;
        }

        @Override
        public BatchImportExecutionRef retry(String executionUUID) {
            return null;
        }
    }
}
