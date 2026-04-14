package customer.batchimportcat.batch.dynamic.types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public class BatchImportProcessResult implements Serializable {
    @Getter
    private final List<BatchImportProcessMessage> messages = new ArrayList<>();
    private boolean hasErrors;

    public boolean hasErrors() {
        return hasErrors;
    }

    public void addMessage(long line, String type, String code, String message, String details) {
        messages.add(new BatchImportProcessMessage(line, type, code, message, details));
        if ("E".equalsIgnoreCase(type) || "A".equalsIgnoreCase(type)) {
            hasErrors = true;
        }
    }

    public void addSuccess(long line, String message) {
        addMessage(line, "S", "SUCCESS", message, null);
    }

    public void addWarning(long line, String message) {
        addMessage(line, "W", "WARNING", message, null);
    }

    public void addError(long line, String code, String message, String details) {
        addMessage(line, "E", code, message, details);
    }
}
