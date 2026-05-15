package customer.batchimportcat.batchtask;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

class BatchImportTaskMainTest {
    @Test
    void runUsesCommandLineOptionsWhenPresent() {
        SingleFileBatchLaunchService launchService = mock(SingleFileBatchLaunchService.class);
        when(launchService.launch("file-1", "execution-1")).thenReturn(true);
        BatchImportTaskMain taskMain = new BatchImportTaskMain(launchService, new MockEnvironment());

        taskMain.run(new DefaultApplicationArguments("--fileUUID=file-1", "--executionUUID=execution-1"));

        assertEquals(0, taskMain.getExitCode());
        verify(launchService).launch("file-1", "execution-1");
    }

    @Test
    void runFallsBackToEnvironmentVariablesWhenCommandLineOptionsAreMissing() {
        SingleFileBatchLaunchService launchService = mock(SingleFileBatchLaunchService.class);
        when(launchService.launch("file-env", "execution-env")).thenReturn(true);
        MockEnvironment environment = new MockEnvironment()
                .withProperty("BATCHIMPORT_FILE_UUID", "file-env")
                .withProperty("BATCHIMPORT_EXECUTION_UUID", "execution-env");
        BatchImportTaskMain taskMain = new BatchImportTaskMain(launchService, environment);

        taskMain.run(new DefaultApplicationArguments());

        assertEquals(0, taskMain.getExitCode());
        verify(launchService).launch("file-env", "execution-env");
    }

    @Test
    void runFailsWhenFileIdentifierIsMissingFromOptionsAndEnvironment() {
        SingleFileBatchLaunchService launchService = mock(SingleFileBatchLaunchService.class);
        MockEnvironment environment = new MockEnvironment()
                .withProperty("BATCHIMPORT_EXECUTION_UUID", "execution-env");
        BatchImportTaskMain taskMain = new BatchImportTaskMain(launchService, environment);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> taskMain.run(new DefaultApplicationArguments()));

        assertEquals("Missing required option --fileUUID or environment variable BATCHIMPORT_FILE_UUID.",
                exception.getMessage());
        assertEquals(1, taskMain.getExitCode());
    }
}
