package customer.batchimportcat.batch.listeners;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

import customer.batchimportcat.batch.itemwriters.ProcessKeyDelegatingStepState;
import customer.batchimportcat.consts.Constant;

public class ProcessKeyDelegatingStepExecutionListener implements StepExecutionListener {
    private final ProcessKeyDelegatingStepState stepState;

    public ProcessKeyDelegatingStepExecutionListener(ProcessKeyDelegatingStepState stepState) {
        this.stepState = stepState;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        stepState.initialize();
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        if (stepState.hasErrors()) {
            stepExecution.getJobExecution()
                    .getExecutionContext()
                    .put(Constant.HAS_PROCESSING_ERRORS, Boolean.TRUE);
        }
        return stepExecution.getExitStatus();
    }
}
