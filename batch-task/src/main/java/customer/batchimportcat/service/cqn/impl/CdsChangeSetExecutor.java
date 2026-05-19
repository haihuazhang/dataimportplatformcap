package customer.batchimportcat.service.cqn.impl;

import java.util.function.Function;
import java.util.function.Supplier;

import com.sap.cds.services.changeset.ChangeSetContext;
import com.sap.cds.services.runtime.CdsRuntime;

final class CdsChangeSetExecutor {
    private CdsChangeSetExecutor() {
    }

    static <T> T runTransactional(CdsRuntime runtime, Supplier<T> supplier) {
        if (ChangeSetContext.isActive()) {
            ChangeSetContext.getCurrent().markTransactional();
            return supplier.get();
        }
        Function<ChangeSetContext, T> action = context -> supplier.get();
        return runtime.changeSetContext().markTransactional().run(action);
    }
}
