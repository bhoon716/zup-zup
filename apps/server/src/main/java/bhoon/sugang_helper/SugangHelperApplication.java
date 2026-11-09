package bhoon.sugang_helper;

import bhoon.sugang_helper.common.api.alerts.SlackAlertProperties;
import bhoon.sugang_helper.common.api.notifications.NotificationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.modulith.Modulithic;

@EnableScheduling
@EnableJpaAuditing
@EnableConfigurationProperties({NotificationProperties.class, SlackAlertProperties.class})
@Modulithic(systemName = "줍줍")
@SpringBootApplication
public class SugangHelperApplication {

    public static void main(String[] args) {
        SpringApplication.run(SugangHelperApplication.class, args);
    }

}
