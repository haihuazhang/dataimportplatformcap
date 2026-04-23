package customer.batchimportcat.batchtask.runtime;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.persistence.PersistenceService;

import customer.batchimportcat.batch.runtime.ProcessorRuntime;

@Component
public class DefaultProcessorRuntime implements ProcessorRuntime {
    private final PersistenceService persistenceService;
    private final ApplicationContext applicationContext;

    public DefaultProcessorRuntime(PersistenceService persistenceService, ApplicationContext applicationContext) {
        this.persistenceService = persistenceService;
        this.applicationContext = applicationContext;
    }

    @Override
    public PersistenceService persistenceService() {
        return persistenceService;
    }

    @Override
    public ApplicationService applicationService(String serviceName) {
        return applicationContext.getBean(serviceName, ApplicationService.class);
    }

    @Override
    public CqnService cqnService(String serviceName) {
        return applicationContext.getBean(serviceName, CqnService.class);
    }
}
