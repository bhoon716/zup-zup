package bhoon.sugang_helper.common.alert;

import bhoon.sugang_helper.common.security.util.SensitiveDataRedactor;
import java.time.Instant;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SlackAlertService {

    private static final int MAX_DEDUPLICATION_KEYS = 256;
    private static final int MAX_STACK_TRACE_FRAMES = 3;
    private final SlackAlertProperties properties;
    private final SlackWebhookClient webhookClient;
    private final long cooldownNanos;
    private final Map<String, Long> lastAlertNanos = new LinkedHashMap<>();

    public SlackAlertService(SlackAlertProperties properties, SlackWebhookClient webhookClient) {
        this.properties = properties;
        this.webhookClient = webhookClient;
        this.cooldownNanos = TimeUnit.SECONDS.toNanos(properties.cooldownSeconds());
    }

    @Async("applicationTaskExecutor")
    public void alert(SlackAlertCategory category, String errorCode, Throwable exception) {
        sendSynchronously(category, errorCode, exception);
    }

    void sendSynchronously(SlackAlertCategory category, String errorCode, Throwable exception) {
        if (properties.webhookUrl().isBlank()) {
            log.debug("Slack alert skipped because webhook is not configured. category={}", category);
            return;
        }

        String deduplicationKey = category + ":" + errorCode + ":" + SensitiveDataRedactor.exceptionType(exception);
        if (!reserveAlertSlot(deduplicationKey, System.nanoTime())) {
            log.debug("Slack alert suppressed during cooldown. category={}, errorCode={}", category, errorCode);
            return;
        }

        String message = formatMessage(category, errorCode, exception);
        try {
            webhookClient.send(message);
        } catch (RuntimeException sendFailure) {
            log.warn("Slack alert delivery failed. category={}, errorCode={}, exceptionType={}",
                    category, errorCode, SensitiveDataRedactor.exceptionType(sendFailure));
        }
    }

    private synchronized boolean reserveAlertSlot(String key, long nowNanos) {
        Long previousNanos = lastAlertNanos.get(key);
        if (previousNanos != null && nowNanos - previousNanos < cooldownNanos) {
            return false;
        }
        if (previousNanos == null && lastAlertNanos.size() >= MAX_DEDUPLICATION_KEYS) {
            Iterator<String> iterator = lastAlertNanos.keySet().iterator();
            iterator.next();
            iterator.remove();
        }
        lastAlertNanos.put(key, nowNanos);
        return true;
    }

    private String formatMessage(SlackAlertCategory category, String errorCode, Throwable exception) {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = "NONE";
        }
        return String.format(
                "environment=%s category=%s occurredAt=%s errorCode=%s exceptionType=%s correlationId=%s stackTraceTop3=%s",
                properties.environment(), category, Instant.now(), errorCode,
                SensitiveDataRedactor.exceptionType(exception), correlationId, stackTraceTop3(exception));
    }

    private String stackTraceTop3(Throwable exception) {
        if (exception == null || exception.getStackTrace().length == 0) {
            return "<none>";
        }
        return Arrays.stream(exception.getStackTrace())
                .limit(MAX_STACK_TRACE_FRAMES)
                .map(StackTraceElement::toString)
                .reduce((left, right) -> left + " | " + right)
                .orElse("<none>");
    }
}
