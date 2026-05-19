package customer.batchimportcat.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TaskLaunchRequestFactoryTest {
    @Test
    void cloudFoundryCommandPrefixesTaskIdentifiersAsEnvironmentVariables() {
        TaskLaunchProperties properties = new TaskLaunchProperties();
        properties.setLauncherType("cloudfoundry");
        properties.getCloudfoundry().setCommandTemplate(
                "{defaultCommand} --fileUUID={fileUUID} --executionUUID={executionUUID}");
        TaskLaunchRequestFactory factory = new TaskLaunchRequestFactory(properties);

        TaskLaunchRequest request = factory.create(
                "execution-1",
                new BatchImportFileLaunchContext("file-1", "config-1", "PROCESS_A", "Object", "Object Name"),
                new ProcessorArtifactBinding("artifact-1", "PROCESS_A", "1.0.0", "Artifact", "abc", "demo.Entry"));

        assertEquals("BATCHIMPORT_FILE_UUID='file-1' BATCHIMPORT_EXECUTION_UUID='execution-1' "
                + "{defaultCommand} --fileUUID=file-1 --executionUUID=execution-1",
                request.command());
    }
}
