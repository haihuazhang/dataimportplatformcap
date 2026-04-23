package customer.batchimportcat.model;

import java.time.Instant;

public record TaskLaunchResult(
        boolean accepted,
        String launcherType,
        String platformTaskId,
        String platformTaskName,
        Instant acceptedAt,
        String rawState,
        String failureReason) {
}
