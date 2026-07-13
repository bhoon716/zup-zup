package bhoon.sugang_helper.notification.infra;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Bounds synchronous provider calls and isolates a failing provider with a small in-memory circuit breaker.
 */
@Component
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.NullAssignment"})
public class NotificationProviderResilience implements AutoCloseable {

    private final MeterRegistry meterRegistry;
    private final Duration timeout;
    private final int failureThreshold;
    private final Duration openDuration;
    private final ExecutorService executor;
    private final Map<NotificationChannel, Circuit> circuits = new ConcurrentHashMap<>();

    public NotificationProviderResilience(
            MeterRegistry meterRegistry,
            @Value("${app.notification.provider.timeout-ms:5000}") long timeoutMs,
            @Value("${app.notification.provider.circuit.failure-threshold:3}") int failureThreshold,
            @Value("${app.notification.provider.circuit.open-seconds:30}") long openSeconds) {
        this(meterRegistry, Duration.ofMillis(timeoutMs), failureThreshold, Duration.ofSeconds(openSeconds),
                Executors.newVirtualThreadPerTaskExecutor());
    }

    NotificationProviderResilience(MeterRegistry meterRegistry, Duration timeout, int failureThreshold,
                                   Duration openDuration, ExecutorService executor) {
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("provider timeout must be positive");
        }
        if (failureThreshold < 1) {
            throw new IllegalArgumentException("provider circuit failure threshold must be positive");
        }
        if (openDuration.isNegative()) {
            throw new IllegalArgumentException("provider circuit open duration must not be negative");
        }
        this.meterRegistry = meterRegistry;
        this.timeout = timeout;
        this.failureThreshold = failureThreshold;
        this.openDuration = openDuration;
        this.executor = executor;
    }

    public void execute(NotificationChannel provider, Runnable operation) {
        Circuit circuit = circuits.computeIfAbsent(provider, ignored -> new Circuit());
        Counter circuitOpenCounter = counter("notification.provider.circuit.open", "provider", provider.name());
        if (!circuit.tryAcquire(Instant.now())) {
            circuitOpenCounter.increment();
            throw new NotificationProviderException(providerErrorCode(provider), true, "CIRCUIT_OPEN", null, null);
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "failure";
        try {
            Future<?> future = executor.submit(operation);
            try {
                future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
                circuit.recordSuccess();
                outcome = "success";
            } catch (TimeoutException exception) {
                future.cancel(true);
                recordFailure(provider, true, "TIMEOUT", null);
                circuit.recordFailure(true, Instant.now(), failureThreshold, openDuration);
                throw new NotificationProviderException(providerErrorCode(provider), true, "TIMEOUT", null, exception);
            } catch (InterruptedException exception) {
                future.cancel(true);
                Thread.currentThread().interrupt();
                recordFailure(provider, true, "INTERRUPTED", null);
                circuit.recordFailure(true, Instant.now(), failureThreshold, openDuration);
                throw new NotificationProviderException(providerErrorCode(provider), true, "INTERRUPTED", null, exception);
            } catch (ExecutionException exception) {
                RuntimeException normalized = normalize(provider, exception.getCause());
                boolean retryable = NotificationFailureClassifier.isRetryable(normalized);
                Integer statusCode = normalized instanceof NotificationProviderException providerException
                        ? providerException.getStatusCode() : null;
                recordFailure(provider, retryable,
                        normalized instanceof NotificationProviderException providerException
                                ? providerException.getReason() : "ERROR", statusCode);
                circuit.recordFailure(retryable, Instant.now(), failureThreshold, openDuration);
                throw normalized;
            }
        } finally {
            Timer timer = Timer.builder("notification.provider.latency")
                    .description("Provider notification call latency")
                    .tag("provider", provider.name())
                    .tag("outcome", outcome)
                    .register(meterRegistry);
            sample.stop(timer);
        }
    }

    private RuntimeException normalize(NotificationChannel provider, Throwable throwable) {
        if (throwable instanceof NotificationProviderException providerException) {
            return providerException;
        }
        if (throwable instanceof CustomException customException) {
            return new NotificationProviderException(
                    customException.getErrorCode(), NotificationFailureClassifier.isRetryable(customException),
                    "ERROR", null, throwable);
        }
        return new NotificationProviderException(providerErrorCode(provider), true, "ERROR", null, throwable);
    }

    private void recordFailure(NotificationChannel provider, boolean retryable, String reason, Integer statusCode) {
        counter("notification.provider.failures", "provider", provider.name(),
                "classification", retryable ? "retryable" : "permanent").increment();
        if ("TIMEOUT".equals(reason)) {
            counter("notification.provider.timeout", "provider", provider.name()).increment();
        }
        if (statusCode != null) {
            counter("notification.provider.http.status", "provider", provider.name(),
                    "status", Integer.toString(statusCode)).increment();
        }
    }

    private Counter counter(String name, String... tags) {
        return Counter.builder(name).tags(tags).register(meterRegistry);
    }

    private ErrorCode providerErrorCode(NotificationChannel provider) {
        return switch (provider) {
            case EMAIL -> ErrorCode.EMAIL_SEND_ERROR;
            case FCM -> ErrorCode.FCM_SEND_ERROR;
            case WEB -> ErrorCode.WEB_PUSH_SEND_ERROR;
            case DISCORD -> ErrorCode.DISCORD_SEND_ERROR;
        };
    }

    @PreDestroy
    @Override
    public void close() {
        executor.shutdownNow();
    }

    private static final class Circuit {

        private int consecutiveFailures;
        private Instant openUntil;
        private boolean probeInFlight;

        synchronized boolean tryAcquire(Instant now) {
            if (openUntil == null) {
                return true;
            }
            if (now.isBefore(openUntil) || probeInFlight) {
                return false;
            }
            probeInFlight = true;
            return true;
        }

        synchronized void recordSuccess() {
            consecutiveFailures = 0;
            openUntil = null;
            probeInFlight = false;
        }

        synchronized void recordFailure(boolean retryable, Instant now, int threshold, Duration duration) {
            if (!retryable) {
                consecutiveFailures = 0;
                probeInFlight = false;
                return;
            }
            consecutiveFailures++;
            if (consecutiveFailures >= threshold) {
                openUntil = now.plus(duration);
            }
            probeInFlight = false;
        }
    }
}
