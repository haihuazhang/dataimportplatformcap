package customer.batchimportcat.quartz.configurations;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import customer.batchimportcat.quartz.jobs.TestJob;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail testJob() {
        return JobBuilder.newJob(TestJob.class).withIdentity("testJob").storeDurably().build();
    }

    @Bean
    public JobDetail testJob2() {
        return JobBuilder.newJob(TestJob.class).withIdentity("testJob2").storeDurably().build();
    }

    @Bean
    public JobDetail testJob3() {
        return JobBuilder.newJob(TestJob.class).withIdentity("testJob3").storeDurably().build();
    }

    // @Bean
    // public Trigger testTrigger(JobDetail testJob) {
    //     SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.repeatSecondlyForTotalCount(5, 30);
    //     return TriggerBuilder.newTrigger().forJob(testJob)
    //             .withIdentity("testTask1")
    //             .withSchedule(scheduleBuilder)
    //             .build();
    // }
}
