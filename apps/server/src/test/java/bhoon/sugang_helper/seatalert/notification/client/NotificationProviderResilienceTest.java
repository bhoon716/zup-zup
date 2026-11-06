package bhoon.sugang_helper.seatalert.notification.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.exception.NotificationProviderException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class NotificationProviderResilienceTest {

    private ExecutorService executor;
    private ExecutorService callers;
    private SimpleMeterRegistry meterRegistry;
    private NotificationProviderResilience resilience;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(4);
        callers = Executors.newFixedThreadPool(2);
        meterRegistry = new SimpleMeterRegistry();
        resilience = new NotificationProviderResilience(
                meterRegistry, Duration.ofMillis(40), 2, Duration.ofSeconds(30), executor);
    }

    @AfterEach
    void tearDown() {
        resilience.close();
        callers.shutdownNow();
        meterRegistry.close();
    }

    @Test
    @DisplayName("제공자 호출 제한 시간을 적용하고 지연·초과 지표를 남긴다")
    void timesOutProviderCallAndRecordsLatencyAndTimeoutMetrics() {
        // when & then
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
    @DisplayName("재시도 가능한 실패가 누적되면 회로를 열고 추가 호출을 거부한다")
    void opensCircuitAfterRetryableFailuresAndRejectsFurtherCalls() {
        // given
        AtomicInteger invocations = new AtomicInteger();
        Runnable outage = () -> {
            invocations.incrementAndGet();
            throw new NotificationProviderException(
                    ErrorCode.EMAIL_SEND_ERROR, true, "OUTAGE", null, null);
        };

        // when & then
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
    @DisplayName("수신자 영구 오류는 회로를 열지 않고 상태 지표를 기록한다")
    void permanentRecipientFailureDoesNotOpenCircuitAndRecordsStatusMetric() {
        // given
        Runnable invalidRecipient = () -> {
            throw new NotificationProviderException(
                    ErrorCode.WEB_PUSH_INVALID_SUBSCRIPTION, false, "INVALID_RECIPIENT", 404, null);
        };

        // when & then
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

    @Test
    @DisplayName("먼저 시작된 영구 실패가 나중의 재시도 오류로 열린 회로를 닫지 않는다")
    void olderPermanentFailureDoesNotCloseCircuitOpenedByNewerRetryableFailure() throws Exception {
        // given
        CountDownLatch permanentStarted = new CountDownLatch(1);
        CountDownLatch releasePermanent = new CountDownLatch(1);
        Runnable olderPermanentFailure = () -> {
            permanentStarted.countDown();
            await(releasePermanent);
            throw new NotificationProviderException(
                    ErrorCode.WEB_PUSH_INVALID_SUBSCRIPTION, false, "INVALID_RECIPIENT", 404, null);
        };
        Runnable retryableFailure = () -> {
            throw new NotificationProviderException(
                    ErrorCode.WEB_PUSH_SEND_ERROR, true, "OUTAGE", 503, null);
        };

        // when
        Future<?> olderFailure = callers.submit(() -> resilience.execute(
                NotificationChannel.WEB, olderPermanentFailure));

        // then
        assertThat(permanentStarted.await(1, TimeUnit.SECONDS)).isTrue();

        assertThatThrownBy(() -> resilience.execute(NotificationChannel.WEB, retryableFailure))
                .isInstanceOf(NotificationProviderException.class);
        assertThatThrownBy(() -> resilience.execute(NotificationChannel.WEB, retryableFailure))
                .isInstanceOf(NotificationProviderException.class);

        releasePermanent.countDown();
        assertThatThrownBy(() -> olderFailure.get(1, TimeUnit.SECONDS))
                .hasCauseInstanceOf(NotificationProviderException.class);

        assertThatThrownBy(() -> resilience.execute(NotificationChannel.WEB, () -> { }))
                .isInstanceOf(NotificationProviderException.class)
                .extracting("reason")
                .isEqualTo("CIRCUIT_OPEN");
    }

    @Test
    @DisplayName("반개방 호출의 영구 오류는 동시 일반 요청 전에 회로를 닫는다")
    void permanentHalfOpenFailureClosesCircuitBeforeConcurrentNormalRequests() throws Exception {
        // given
        ExecutorService halfOpenExecutor = Executors.newFixedThreadPool(4);
        NotificationProviderResilience halfOpenResilience = new NotificationProviderResilience(
                meterRegistry, Duration.ofMillis(40), 2, Duration.ZERO, halfOpenExecutor);
        NotificationProviderException outage = new NotificationProviderException(
                ErrorCode.WEB_PUSH_SEND_ERROR, true, "OUTAGE", null, null);
        Runnable permanentFailure = () -> {
            throw new NotificationProviderException(
                    ErrorCode.WEB_PUSH_INVALID_SUBSCRIPTION, false, "INVALID_RECIPIENT", 404, null);
        };

        // when & then
        try {
            assertThatThrownBy(() -> halfOpenResilience.execute(NotificationChannel.WEB, () -> {
                throw outage;
            })).isSameAs(outage);
            assertThatThrownBy(() -> halfOpenResilience.execute(NotificationChannel.WEB, () -> {
                throw outage;
            })).isSameAs(outage);
            assertThatThrownBy(() -> halfOpenResilience.execute(NotificationChannel.WEB, permanentFailure))
                    .isInstanceOf(NotificationProviderException.class);

            CountDownLatch normalStarted = new CountDownLatch(1);
            CountDownLatch releaseNormal = new CountDownLatch(1);
            Future<?> firstNormal = callers.submit(() -> halfOpenResilience.execute(NotificationChannel.WEB, () -> {
                normalStarted.countDown();
                await(releaseNormal);
            }));

            assertThat(normalStarted.await(1, TimeUnit.SECONDS)).isTrue();
            Future<?> concurrentNormal = callers.submit(() -> halfOpenResilience.execute(NotificationChannel.WEB, () -> { }));

            try {
                assertThatCode(() -> concurrentNormal.get(1, TimeUnit.SECONDS)).doesNotThrowAnyException();
            } finally {
                releaseNormal.countDown();
            }
            assertThatCode(() -> firstNormal.get(1, TimeUnit.SECONDS)).doesNotThrowAnyException();
        } finally {
            halfOpenResilience.close();
        }
    }

    @Test
    @DisplayName("제공자 작업에 추적 ID를 전달하고 작업자 문맥을 정리한다")
    void propagatesCorrelationIdToProviderTaskAndClearsWorkerContext() {
        // given
        AtomicReference<String> observedCorrelationId = new AtomicReference<>();
        AtomicReference<String> leakedWorkerValue = new AtomicReference<>();

        MDC.put("correlationId", "request-123");

        // when
        resilience.execute(NotificationChannel.EMAIL, () -> {
            observedCorrelationId.set(MDC.get("correlationId"));
            MDC.put("worker-only", "must-be-cleared");
        });
        MDC.clear();

        resilience.execute(NotificationChannel.EMAIL,
                () -> leakedWorkerValue.set(MDC.get("worker-only")));

        // then
        Assertions.assertThat(observedCorrelationId).hasValue("request-123");
        Assertions.assertThat(leakedWorkerValue.get()).isNull();
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(exception);
        }
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(1, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting for test operation release");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for test operation release", exception);
        }
    }
}
