package customer.batchimportcat.batch.exceptions;

public class BatchFileNotFound extends RuntimeException {
    public BatchFileNotFound(String message) {
        super(message);
    }
}
