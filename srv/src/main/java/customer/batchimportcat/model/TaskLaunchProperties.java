package customer.batchimportcat.model;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "batchimport.task")
public class TaskLaunchProperties {
    private String launcherType = "local";
    private String hostApp = "batchimportcat-batch-task";
    private Integer memoryMb = 1024;
    private Integer diskMb = 1024;
    private Duration timeout = Duration.ofHours(1);
    private Map<String, String> environment = new LinkedHashMap<>();
    private Local local = new Local();
    private CloudFoundry cloudfoundry = new CloudFoundry();

    @Getter
    @Setter
    public static class Local {
        private String shell = "/bin/zsh";
        private String batchTaskJar = "batch-task/target/batchimportcat-batch-task-exec.jar";
        private String javaBinary;
        private String commandTemplate;
    }

    @Getter
    @Setter
    public static class CloudFoundry {
        private String cfBinary = "cf";
        private String commandTemplate =
                "java -jar app.jar --fileUUID={fileUUID} --executionUUID={executionUUID}";
    }
}
