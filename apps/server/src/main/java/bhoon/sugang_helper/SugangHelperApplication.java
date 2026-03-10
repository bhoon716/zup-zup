package bhoon.sugang_helper;

import bhoon.sugang_helper.common.config.NotificationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@EnableJpaAuditing
@EnableConfigurationProperties(NotificationProperties.class)
@SpringBootApplication
public class SugangHelperApplication {

    public static void main(String[] args) {
        SpringApplication.run(SugangHelperApplication.class, args);
    }

}
