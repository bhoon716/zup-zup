package bhoon.sugang_helper.notification.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bhoon.sugang_helper.common.error.ErrorCode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class NotificationProviderResilienceTest {

    private ExecutorService executor;
    private SimpleMeterRegistry meterRegistry;
    private NotificationProviderResilience resilience;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(4);
        meterRegistry = new SimpleMeterRegistry();
        resilience = new NotificationProviderResilience(
                meterRegistry, Duration.ofMillis(40), 2, Duration.ofSeconds(30), executor);
    }

    @AfterEach
    void tearDown() {
        resilience.close();
        meterRegistry.close();
    }

    @Test
    void timesOutProviderCallAndRecordsLatencyAndTimeoutMetrics() {
        assertThatThrownBy(() -> resilience.execute(NotificationChannel.EMAIL,
                () -> sleep(Duration.ofMillis(200))))
                .isInstanceOf(NotificationProviderException.class)
                .extracting("retryable")
                .isEqualTo(true);

        assertThat(meterRegistry.get("notification.provider.timeout")
                .tag("provider", "EMAIL").counter().count()).isEqualTo(1);
        assertThat(meterRegistry.get("notification.provider.latency")
                .tag("provider", "EMAIL").timer().count()).isEqualTo(1);
    }

    @Test
    void opensCircuitAfterRetryableFailuresAndRejectsFurtherCalls() {
        AtomicInteger invocations = new AtomicInteger();
        Runnable outage = () -> {
            invocations.incrementAndGet();
            throw new NotificationProviderException(
                    ErrorCode.EMAIL_SEND_ERROR, true, "OUTAGE", null, null);
        };

        assertThatThrownBy(() -> resilience.execute(NotificationChannel.EMAIL, outage))
                .isInstanceOf(NotificationProviderException.class);
        assertThatThrownBy(() -> resilience.execute(NotificationChannel.EMAIL, outage))
                .isInstanceOf(NotificationProviderException.class);
        assertThatThrownBy(() -> resilience.execute(NotificationChannel.EMAIL, outage))
                .isInstanceOf(NotificationProviderException.class)
                .extracting("reason")
                .isEqualTo("CIRCUIT_OPEN");

        assertThat(invocations).hasValue(2);
        assertThat(meterRegistry.get("notification.provider.circuit.open")
                .tag("provider", "EMAIL").counter().count()).isEqualTo(1);
    }

    @Test
    void permanentRecipientFailureDoesNotOpenCircuitAndRecordsStatusMetric() {
        Runnable invalidRecipient = () -> {
            throw new NotificationProviderException(
                    ErrorCode.WEB_PUSH_INVALID_SUBSCRIPTION, false, "INVALID_RECIPIENT", 404, null);
        };

        assertThatThrownBy(() -> resilience.execute(NotificationChannel.WEB, invalidRecipient))
                .isInstanceOf(NotificationProviderException.class);
        assertThatThrownBy(() -> resilience.execute(NotificationChannel.WEB, invalidRecipient))
                .isInstanceOf(NotificationProviderException.class);

        assertThat(meterRegistry.get("notification.provider.circuit.open")
                .tag("provider", "WEB").counter().count()).isZero();
        assertThat(meterRegistry.get("notification.provider.http.status")
                .tag("provider", "WEB").tag("status", "404").counter().count()).isEqualTo(2);
        assertThat(meterRegistry.get("notification.provider.failures")
                .tag("provider", "WEB").tag("classification", "permanent").counter().count()).isEqualTo(2);
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(exception);
        }
    }
}
