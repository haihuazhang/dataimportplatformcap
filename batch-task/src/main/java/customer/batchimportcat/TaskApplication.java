package customer.batchimportcat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class TaskApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(TaskApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext context = application.run(args);
        int exitCode = SpringApplication.exit(context);
        System.exit(exitCode);
    }
}
