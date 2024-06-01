package customer.batchimportcat.batch.exceptions;

public class BatchConfigNotFound extends RuntimeException {

    public BatchConfigNotFound(String message) {
        super(message);
    }

    public BatchConfigNotFound(Throwable cause ) {
        super(cause);
    }
}
