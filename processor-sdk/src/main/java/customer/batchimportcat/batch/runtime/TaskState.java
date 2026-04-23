package customer.batchimportcat.batch.runtime;

public enum TaskState {
    SUBMITTED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED;

    public String value() {
        return name();
    }
}
