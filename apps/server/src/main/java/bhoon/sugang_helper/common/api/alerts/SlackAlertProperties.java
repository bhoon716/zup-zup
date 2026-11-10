package bhoon.sugang_helper.common.api.alerts;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "jbnu.slack")
public record SlackAlertProperties(
        String webhookUrl,
        String environment,
        long cooldownSeconds,
        long timeoutMs) {

    private static final long DEFAULT_COOLDOWN_SECONDS = 60;
    private static final long DEFAULT_TIMEOUT_MS = 2_000;

    public SlackAlertProperties(String webhookUrl, String environment, long cooldownSeconds, long timeoutMs) {
        this.webhookUrl = webhookUrl == null ? "" : webhookUrl.trim();
        this.environment = !StringUtils.hasText(environment) ? "UNKNOWN" : environment.trim();
        this.cooldownSeconds = cooldownSeconds < 1 ? DEFAULT_COOLDOWN_SECONDS : cooldownSeconds;
        this.timeoutMs = timeoutMs < 1 ? DEFAULT_TIMEOUT_MS : timeoutMs;
    }
}
