package customer.batchimportcat.handlers;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.sap.cds.services.EventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.ServiceName;

import cds.gen.dataimportservice.BatchImportConfig;
import cds.gen.dataimportservice.BatchImportConfig_;
import cds.gen.dataimportservice.BatchImportStructure;
import cds.gen.dataimportservice.BatchImportStructure_;
import cds.gen.dataimportservice.DataImportService_;
import customer.batchimportcat.batch.dynamic.DynamicConfigurationBuilder;
import customer.batchimportcat.batch.dynamic.types.DynamicImportConfiguration;
import customer.batchimportcat.consts.Constant;
import customer.batchimportcat.service.BatchImportTemplateService;
import customer.batchimportcat.service.cqn.BatchImportConfigCqnService;
import customer.batchimportcat.service.cqn.BatchImportFieldCqnService;
import customer.batchimportcat.service.cqn.BatchImportStructureCqnService;

@Component
@ServiceName(DataImportService_.CDS_NAME)
public class BatchImportConfigHandler implements EventHandler {
    private final BatchImportConfigCqnService batchImportConfigCqnService;
    private final BatchImportStructureCqnService batchImportStructureCqnService;
    private final BatchImportFieldCqnService batchImportFieldCqnService;
    private final DynamicConfigurationBuilder dynamicConfigurationBuilder;
    private final BatchImportTemplateService batchImportTemplateService;

    public BatchImportConfigHandler(BatchImportConfigCqnService batchImportConfigCqnService,
            BatchImportStructureCqnService batchImportStructureCqnService,
            BatchImportFieldCqnService batchImportFieldCqnService,
            DynamicConfigurationBuilder dynamicConfigurationBuilder,
            BatchImportTemplateService batchImportTemplateService) {
        this.batchImportConfigCqnService = batchImportConfigCqnService;
        this.batchImportStructureCqnService = batchImportStructureCqnService;
        this.batchImportFieldCqnService = batchImportFieldCqnService;
        this.dynamicConfigurationBuilder = dynamicConfigurationBuilder;
        this.batchImportTemplateService = batchImportTemplateService;
    }

    @After(event = { "CREATE", "UPDATE" }, entity = BatchImportConfig_.CDS_NAME)
    public void refreshTemplate(Stream<BatchImportConfig> configs, EventContext conntext) {
        configs.forEach(config -> {
            if (config == null || config.getId() == null || Boolean.FALSE.equals(config.getIsActiveEntity())) {
                return;
            }
            batchImportStructureCqnService.ensureSingleRootStructure(config.getId(), true);

            DynamicImportConfiguration dynamicConfig = loadDynamicConfiguration(config.getId());
            byte[] template = batchImportTemplateService.generateTemplate(dynamicConfig);
            batchImportConfigCqnService.updateTemplateWithDb(
                    config.getId(),
                    template,
                    Constant.TEMPLATE_MIME_TYPE,
                    buildTemplateFileName(dynamicConfig));
        });
    }

    @Before(event = { "CREATE", "UPDATE" }, entity = BatchImportStructure_.CDS_NAME)
    public void validateSingleRootStructure(Stream<BatchImportStructure> structures) {
        structures.forEach(batchImportStructureCqnService::validateSingleRootStructure);
    }

    private DynamicImportConfiguration loadDynamicConfiguration(String configUUID) {
        Map<String, Serializable> configRow = batchImportConfigCqnService.loadRequiredSerializable(configUUID);
        List<Map<String, Serializable>> structureRows = batchImportStructureCqnService
                .loadSerializableByConfigUUID(configUUID);
        List<Map<String, Serializable>> fieldRows = batchImportFieldCqnService.loadSerializableByConfigUUID(configUUID);
        return dynamicConfigurationBuilder.build(configRow, structureRows, fieldRows);
    }

    private String buildTemplateFileName(DynamicImportConfiguration configuration) {
        String objectCode = configuration.object() == null || configuration.object().isBlank()
                ? configuration.id()
                : configuration.object();
        return objectCode + "_Template.xlsx";
    }
}
