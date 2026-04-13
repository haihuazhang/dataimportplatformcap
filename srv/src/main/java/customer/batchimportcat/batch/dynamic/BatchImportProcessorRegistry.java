package customer.batchimportcat.batch.dynamic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

import customer.batchimportcat.batch.exceptions.BatchConfigNotFound;

@Component
public class BatchImportProcessorRegistry {
    private final Map<String, BatchImportProcessor> processorsByKey = new LinkedHashMap<>();

    public BatchImportProcessorRegistry(List<BatchImportProcessor> processors) {
        for (BatchImportProcessor processor : processors) {
            String processKey = processor.getProcessKey();
            if (processKey == null || processKey.isBlank()) {
                throw new IllegalStateException("BatchImportProcessor process key must not be blank.");
            }
            BatchImportProcessor previous = processorsByKey.putIfAbsent(processKey, processor);
            if (previous != null) {
                throw new IllegalStateException("Duplicate BatchImportProcessor process key: " + processKey);
            }
        }
    }

    public BatchImportProcessor getRequired(String processKey) {
        BatchImportProcessor processor = processorsByKey.get(processKey);
        if (processor == null) {
            throw new BatchConfigNotFound("Process key is not maintained in config " + processKey);
        }
        return processor;
    }

    public List<Map<String, Object>> getValueHelps() {
        List<Map<String, Object>> data = new ArrayList<>();
        for (BatchImportProcessor processor : processorsByKey.values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("ProcessKey", processor.getProcessKey());
            row.put("Description", processor.getDescription());
            data.add(row);
        }
        return data;
    }

    public List<Map<String, Object>> getImplementedClasses() {
        List<Map<String, Object>> data = new ArrayList<>();
        for (BatchImportProcessor processor : processorsByKey.values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("Name", processor.getClass().getName());
            row.put("Description", Objects.requireNonNullElse(processor.getDescription(), processor.getClass().getSimpleName()));
            data.add(row);
        }
        return data;
    }
}
