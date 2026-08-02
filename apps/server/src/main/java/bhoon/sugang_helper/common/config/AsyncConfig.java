package bhoon.sugang_helper.common.config;

import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

    @Bean(name = "applicationTaskExecutor")
    public ThreadPoolTaskExecutor applicationTaskExecutor(
            ThreadPoolTaskExecutorBuilder builder,
            MdcTaskDecorator mdcTaskDecorator) {
        return builder
                .threadNamePrefix("application-async-")
                .taskDecorator(mdcTaskDecorator)
                .build();
    }
}
