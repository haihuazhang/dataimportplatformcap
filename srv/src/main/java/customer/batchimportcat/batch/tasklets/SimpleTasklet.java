package customer.batchimportcat.batch.tasklets;

import java.util.concurrent.TimeUnit;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

public class SimpleTasklet implements Tasklet {

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext context) throws Exception {
        // TODO Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'execute'");
        System.out.println(System.nanoTime());
        TimeUnit.SECONDS.sleep(60);

        return RepeatStatus.FINISHED;
    }

}
