package customer.batchimportcat.service.cqn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sap.cds.Result;
import com.sap.cds.Row;
import com.sap.cds.ql.Select;

import cds.gen.dataimportservice.BatchImportField_;
import cds.gen.dataimportservice.DataImportService;
// import cds.gen.zzdt.BatchImportField_;

@Service
public class BatchImportFieldCqnService {
    private final DataImportService dataImportService;

    public BatchImportFieldCqnService( DataImportService dataImportService) {
        this.dataImportService = dataImportService;
    }

    public List<Map<String, Serializable>> loadSerializableByConfigUUID(String configUUID) {
        Result result = dataImportService.run(
                Select.from(BatchImportField_.class)
                        .where(field -> field.ConfigUUID().eq(configUUID)));
        return toSerializableRows(result);
    }

    private List<Map<String, Serializable>> toSerializableRows(Result result) {
        List<Map<String, Serializable>> rows = new ArrayList<>();
        for (Row row : result.listOf(Row.class)) {
            rows.add(toSerializableMap(row));
        }
        return rows;
    }

    private Map<String, Serializable> toSerializableMap(Row row) {
        Map<String, Serializable> serializableData = new HashMap<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getValue() instanceof Serializable serializable) {
                serializableData.put(entry.getKey(), serializable);
            }
        }
        return serializableData;
    }
}
