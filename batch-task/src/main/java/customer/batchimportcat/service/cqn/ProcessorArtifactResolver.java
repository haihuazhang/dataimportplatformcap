package customer.batchimportcat.service.cqn;

import customer.batchimportcat.batchtask.runtime.ResolvedProcessorArtifact;

public interface ProcessorArtifactResolver {
    ResolvedProcessorArtifact resolve(String executionUUID);
}
