package bhoon.sugang_helper.seatalert.notification.client;

import bhoon.sugang_helper.common.api.concurrent.MdcTaskDecorator;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.exception.NotificationProviderException;
import bhoon.sugang_helper.seatalert.notification.service.NotificationFailureClassifier;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.NullAssignment"})
public class NotificationProviderResilience implements AutoCloseable {

    private static final String ERROR_REASON = "ERROR";
    private static final String TIMEOUT_REASON = "TIMEOUT";
    private static final String PROVIDER_TAG = "provider";

    private final MeterRegistry meterRegistry;
    private final Duration timeout;
    private final int failureThreshold;
    private final Duration openDuration;
    private final ExecutorService executor;
    private final MdcTaskDecorator mdcTaskDecorator;
    private final Map<NotificationChannel, Circuit> circuits = new ConcurrentHashMap<>();

    @Autowired
    public NotificationProviderResilience(
            MeterRegistry meterRegistry,
            @Value("${app.notification.provider.timeout-ms:5000}") long timeoutMs,
            @Value("${app.notification.provider.circuit.failure-threshold:3}") int failureThreshold,
            @Value("${app.notification.provider.circuit.open-seconds:30}") long openSeconds,
            MdcTaskDecorator mdcTaskDecorator) {
        this(meterRegistry, Duration.ofMillis(timeoutMs), failureThreshold, Duration.ofSeconds(openSeconds),
                Executors.newVirtualThreadPerTaskExecutor(), mdcTaskDecorator);
    }

    NotificationProviderResilience(MeterRegistry meterRegistry, Duration timeout, int failureThreshold,
                                   Duration openDuration, ExecutorService executor) {
        this(meterRegistry, timeout, failureThreshold, openDuration, executor, new MdcTaskDecorator());
    }

    NotificationProviderResilience(MeterRegistry meterRegistry, Duration timeout, int failureThreshold,
                                   Duration openDuration, ExecutorService executor,
                                   MdcTaskDecorator mdcTaskDecorator) {
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("알림 발송 제공자의 요청 제한 시간은 0보다 커야 합니다.");
        }
        if (failureThreshold < 1) {
            throw new IllegalArgumentException("알림 발송 회로 차단의 실패 횟수 기준은 0보다 커야 합니다.");
        }
        if (openDuration.isNegative()) {
            throw new IllegalArgumentException("알림 발송 회로 차단 유지 시간은 음수일 수 없습니다.");
        }
        this.meterRegistry = meterRegistry;
        this.timeout = timeout;
        this.failureThreshold = failureThreshold;
        this.openDuration = openDuration;
        this.executor = executor;
        this.mdcTaskDecorator = mdcTaskDecorator;
    }

    public void execute(NotificationChannel provider, Runnable operation) {
        Circuit circuit = circuits.computeIfAbsent(provider, ignored -> new Circuit());
        Counter circuitOpenCounter = counter("notification.provider.circuit.open", PROVIDER_TAG, provider.name());
        if (!circuit.tryAcquire(Instant.now())) {
            circuitOpenCounter.increment();
            throw new NotificationProviderException(providerErrorCode(provider), true, "CIRCUIT_OPEN", null, null);
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "failure";
        try {
            Future<?> future = executor.submit(mdcTaskDecorator.decorate(operation));
            awaitProviderResult(provider, circuit, future);
            outcome = "success";
        } finally {
            recordLatency(provider, outcome, sample);
        }
    }

    private void awaitProviderResult(NotificationChannel provider, Circuit circuit, Future<?> future) {
        try {
            future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            circuit.recordSuccess();
        } catch (TimeoutException | InterruptedException exception) {
            future.cancel(true);
            String reason = TIMEOUT_REASON;
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                reason = "INTERRUPTED";
            }
            recordFailure(provider, true, reason, null);
            circuit.recordFailure(true, Instant.now(), failureThreshold, openDuration);
            throw new NotificationProviderException(providerErrorCode(provider), true, reason, null, exception);
        } catch (ExecutionException exception) {
            NotificationProviderException normalized = normalize(provider, exception.getCause());
            boolean retryable = normalized.isRetryable();
            recordFailure(provider, retryable, normalized.getReason(), normalized.getStatusCode());
            circuit.recordFailure(retryable, Instant.now(), failureThreshold, openDuration);
            throw normalized;
        }
    }

    private void recordLatency(NotificationChannel provider, String outcome, Timer.Sample sample) {
        Timer timer = Timer.builder("notification.provider.latency")
                .description("알림 발송 제공자 호출 지연 시간")
                .tag(PROVIDER_TAG, provider.name())
                .tag("outcome", outcome)
                .register(meterRegistry);
        sample.stop(timer);
    }

    private NotificationProviderException normalize(NotificationChannel provider, Throwable throwable) {
        if (throwable instanceof NotificationProviderException providerException) {
            return providerException;
        }
        if (throwable instanceof CustomException customException) {
            return new NotificationProviderException(
                    customException.getErrorCode(), NotificationFailureClassifier.isRetryable(customException),
                    ERROR_REASON, null, throwable);
        }
        return new NotificationProviderException(providerErrorCode(provider), true, ERROR_REASON, null, throwable);
    }

    private void recordFailure(NotificationChannel provider, boolean retryable, String reason, Integer statusCode) {
        counter("notification.provider.failures", PROVIDER_TAG, provider.name(),
                "classification", retryable ? "retryable" : "permanent").increment();
        if (TIMEOUT_REASON.equals(reason)) {
            counter("notification.provider.timeout", PROVIDER_TAG, provider.name()).increment();
        }
        if (statusCode != null) {
            counter("notification.provider.http.status", PROVIDER_TAG, provider.name(),
                    "status", Integer.toString(statusCode)).increment();
        }
    }

    private Counter counter(String name, String... tags) {
        return Counter.builder(name).tags(tags).register(meterRegistry);
    }

    private ErrorCode providerErrorCode(NotificationChannel provider) {
        return switch (provider) {
            case EMAIL -> ErrorCode.EMAIL_SEND_ERROR;
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
                if (openUntil == null || !now.isBefore(openUntil)) {
                    openUntil = null;
                }
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
