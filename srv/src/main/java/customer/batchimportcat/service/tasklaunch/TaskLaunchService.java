package customer.batchimportcat.service.tasklaunch;

import customer.batchimportcat.model.TaskLaunchRequest;
import customer.batchimportcat.model.TaskLaunchResult;

public interface TaskLaunchService {
    TaskLaunchResult launch(TaskLaunchRequest request);
}
