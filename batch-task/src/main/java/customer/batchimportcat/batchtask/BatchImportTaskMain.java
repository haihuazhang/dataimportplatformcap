package customer.batchimportcat.batchtask;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

@Component
public class BatchImportTaskMain implements ApplicationRunner, ExitCodeGenerator {
    private final SingleFileBatchLaunchService singleFileBatchLaunchService;
    private int exitCode = 0;

    public BatchImportTaskMain(SingleFileBatchLaunchService singleFileBatchLaunchService) {
        this.singleFileBatchLaunchService = singleFileBatchLaunchService;
    }

    @Override
    public void run(ApplicationArguments args) {
        String fileUUID = getRequiredOption(args, "fileUUID");
        String executionUUID = getRequiredOption(args, "executionUUID");

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

    private String getRequiredOption(ApplicationArguments args, String optionName) {
        if (!args.containsOption(optionName) || args.getOptionValues(optionName) == null
                || args.getOptionValues(optionName).isEmpty()) {
            exitCode = 1;
            throw new IllegalArgumentException("Missing required option --" + optionName + ".");
        }
        return args.getOptionValues(optionName).get(0);
    }
}
