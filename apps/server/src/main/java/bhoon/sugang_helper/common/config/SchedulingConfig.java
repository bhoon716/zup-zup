package bhoon.sugang_helper.common.config;

import org.springframework.boot.task.ThreadPoolTaskSchedulerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulingConfig {

    @Bean
    public ThreadPoolTaskScheduler monitoringTaskScheduler(ThreadPoolTaskSchedulerBuilder builder) {
        return builder.poolSize(1).threadNamePrefix("monitoring-").build();
    }

    @Bean
    public ThreadPoolTaskScheduler notificationTaskScheduler(ThreadPoolTaskSchedulerBuilder builder) {
        return builder.poolSize(1).threadNamePrefix("notification-").build();
    }

    @Bean
    public ThreadPoolTaskScheduler taskScheduler(ThreadPoolTaskSchedulerBuilder builder) {
        return builder.poolSize(1).build();
    }
}
