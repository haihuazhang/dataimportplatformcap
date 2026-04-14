package customer.batchimportcat.handlers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.sap.cds.Result;
import com.sap.cds.Row;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.CqnUpdate;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;

import cds.gen.dataimportservice.BatchImportConfig;
import cds.gen.dataimportservice.BatchImportConfig_;
import cds.gen.dataimportservice.BatchImportField_;
import cds.gen.dataimportservice.BatchImportStructure_;
import cds.gen.dataimportservice.DataImportService_;
import customer.batchimportcat.batch.dynamic.DynamicConfigurationBuilder;
import customer.batchimportcat.batch.dynamic.types.DynamicImportConfiguration;
import customer.batchimportcat.consts.Constant;
import customer.batchimportcat.service.BatchImportTemplateService;

@Component
@ServiceName(DataImportService_.CDS_NAME)
public class BatchImportConfigHandler implements EventHandler {
    private final PersistenceService db;
    private final DynamicConfigurationBuilder dynamicConfigurationBuilder;
    private final BatchImportTemplateService batchImportTemplateService;

    public BatchImportConfigHandler(PersistenceService db,
            DynamicConfigurationBuilder dynamicConfigurationBuilder,
            BatchImportTemplateService batchImportTemplateService) {
        this.db = db;
        this.dynamicConfigurationBuilder = dynamicConfigurationBuilder;
        this.batchImportTemplateService = batchImportTemplateService;
    }

    @After(event = { "CREATE", "UPDATE" }, entity = BatchImportConfig_.CDS_NAME)
    public void refreshTemplate(Stream<BatchImportConfig> configs) {
        configs.forEach(config -> {
            if (config == null || config.getId() == null || Boolean.FALSE.equals(config.getIsActiveEntity())) {
                return;
            }
            DynamicImportConfiguration dynamicConfig = loadDynamicConfiguration(config.getId());
            byte[] template = batchImportTemplateService.generateTemplate(dynamicConfig);
            Map<String, Object> data = new HashMap<>();
            data.put("Template", template);
            data.put("MimeType", Constant.TEMPLATE_MIME_TYPE);
            data.put("FileName", buildTemplateFileName(dynamicConfig));

            CqnUpdate update = Update.entity(BatchImportConfig_.class)
                    .data(data)
                    .where(row -> row.ID().eq(config.getId()));
            db.run(update);
        });
    }

    private DynamicImportConfiguration loadDynamicConfiguration(String configUUID) {
        Result configResult = db.run(
                Select.from(BatchImportConfig_.class).where(config -> config.ID().eq(configUUID)));
        Row configRow = configResult.first().orElseThrow();
        List<Map<String, Serializable>> structureRows = toSerializableRows(db.run(
                Select.from(BatchImportStructure_.class)
                        .where(structure -> structure.ConfigUUID().eq(configUUID))));
        List<Map<String, Serializable>> fieldRows = toSerializableRows(db.run(
                Select.from(BatchImportField_.class)
                        .where(field -> field.ConfigUUID().eq(configUUID))));
        return dynamicConfigurationBuilder.build(toSerializableMap(configRow), structureRows, fieldRows);
    }

    private String buildTemplateFileName(DynamicImportConfiguration configuration) {
        String objectCode = configuration.object() == null || configuration.object().isBlank()
                ? configuration.id()
                : configuration.object();
        return objectCode + "_Template.xlsx";
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
