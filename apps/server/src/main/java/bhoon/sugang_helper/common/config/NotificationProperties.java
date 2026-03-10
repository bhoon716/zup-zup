package bhoon.sugang_helper.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notification.channels")
public record NotificationProperties(
        boolean email,
        boolean discord,
        boolean webpush,
        boolean fcm) {
}
