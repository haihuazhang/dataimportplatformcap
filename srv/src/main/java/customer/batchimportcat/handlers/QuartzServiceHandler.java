package customer.batchimportcat.handlers;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

import cds.gen.quartzservice.QuartzService_;
import cds.gen.quartzservice.SimpleQuartzTriggerContext;

@Component
@ServiceName(QuartzService_.CDS_NAME)
public class QuartzServiceHandler implements EventHandler {

    @Autowired
    Scheduler stdScheduler;

    @Autowired
    @Qualifier("testJob")
    JobDetail testJob;

    @On(event = SimpleQuartzTriggerContext.CDS_NAME)
    public void simpleTrigger(SimpleQuartzTriggerContext cTriggerContext) throws SchedulerException {

        // TriggerKey triggerKey = TriggerKey.triggerKey("testTask1", "DEFAULT");
        // if
        // (stdScheduler.getTriggerState(triggerKey).equals(Trigger.TriggerState.ERROR))
        // {
        // stdScheduler.resetTriggerFromErrorState(triggerKey);
        // }
        // stdScheduler.scheduleJob(testJob, null)

        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.repeatSecondlyForTotalCount(5, 30);
        Trigger trigger = TriggerBuilder.newTrigger().forJob(testJob)
                .withIdentity("testTask1")
                .withSchedule(scheduleBuilder)
                .build();

        stdScheduler.scheduleJob(trigger);
        // cTriggerContext.setCompleted();
        cTriggerContext.setResult(true);


    }

}
