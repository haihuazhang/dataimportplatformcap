package customer.batchimportcat.batch.runtime;

import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.persistence.PersistenceService;

public interface ProcessorRuntime {
    PersistenceService persistenceService();

    ApplicationService applicationService(String serviceName);

    CqnService cqnService(String serviceName);
}
