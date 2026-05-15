package customer.batchimportcat.batchtask;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class BatchImportTaskMain implements ApplicationRunner, ExitCodeGenerator {
    private static final String FILE_UUID_OPTION = "fileUUID";
    private static final String EXECUTION_UUID_OPTION = "executionUUID";
    private static final String FILE_UUID_ENV = "BATCHIMPORT_FILE_UUID";
    private static final String EXECUTION_UUID_ENV = "BATCHIMPORT_EXECUTION_UUID";

    private final SingleFileBatchLaunchService singleFileBatchLaunchService;
    private final Environment environment;
    private int exitCode = 0;

    public BatchImportTaskMain(SingleFileBatchLaunchService singleFileBatchLaunchService, Environment environment) {
        this.singleFileBatchLaunchService = singleFileBatchLaunchService;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String fileUUID = getRequiredValue(args, FILE_UUID_OPTION, FILE_UUID_ENV);
        String executionUUID = getRequiredValue(args, EXECUTION_UUID_OPTION, EXECUTION_UUID_ENV);

        boolean success = singleFileBatchLaunchService.launch(fileUUID, executionUUID);
        exitCode = success ? 0 : 1;
        if (!success) {
            throw new IllegalStateException("Batch task execution failed for execution " + executionUUID + ".");
        }
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

    private String getRequiredValue(ApplicationArguments args, String optionName, String environmentVariableName) {
        String optionValue = getOptionValue(args, optionName);
        if (optionValue != null && !optionValue.isBlank()) {
            return optionValue;
        }

        String environmentValue = environment.getProperty(environmentVariableName);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue;
        }

        exitCode = 1;
        throw new IllegalArgumentException("Missing required option --" + optionName
                + " or environment variable " + environmentVariableName + ".");
    }

    private String getOptionValue(ApplicationArguments args, String optionName) {
        if (!args.containsOption(optionName) || args.getOptionValues(optionName) == null
                || args.getOptionValues(optionName).isEmpty()) {
            return null;
        }
        return args.getOptionValues(optionName).get(0);
    }
}
