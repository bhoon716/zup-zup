package bhoon.sugang_helper.common.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.common.api.alerts.SlackAlertCategory;
import bhoon.sugang_helper.common.api.alerts.SlackAlertProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class SlackAlertDeliveryTest {

    private static final SlackAlertProperties PROPERTIES =
            new SlackAlertProperties("https://hooks.slack.com/services/test", "TEST", 60, 2_000);
    private static final SlackAlertCategory CATEGORY = SlackAlertCategory.SERVER_5XX;
    private static final String ERROR_CODE = "G001";
    private static final String REJECTED = "rejected";
    private static final String SUCCESS = "success";
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @Test
    @DisplayName("실제 실행기는 호출자를 막지 않고 큐를 제한하며 호출 당시 추적 ID를 보존한다")
    void boundsAsyncQueueAndCapturesCallerContext() throws InterruptedException {
        // given
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<String> firstMessage = new AtomicReference<>();
        AtomicReference<Thread> deliveryThread = new AtomicReference<>();
        SlackWebhookClient client = message -> {
            firstMessage.compareAndSet(null, message);
            deliveryThread.compareAndSet(null, Thread.currentThread());
            started.countDown();
            try {
                release.await();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        };
        SlackAlertService service = new SlackAlertService(PROPERTIES, client, registry);
        try {
            // when
            MDC.put("correlationId", "request-before-queue");
            service.alert(CATEGORY, ERROR_CODE, failureAt(0));
            assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
            MDC.put("correlationId", "request-after-queue");
            for (int site = 1; site <= 65; site++) {
                service.alert(CATEGORY, ERROR_CODE, failureAt(site));
            }

            // then
            assertThat(deliveryThread.get()).isNotSameAs(Thread.currentThread());
            assertThat(firstMessage.get()).contains("요청=request-before-queue")
                    .doesNotContain("request-after-queue");
            assertThat(count(REJECTED)).isEqualTo(1);
            assertThat(count(SUCCESS)).isZero();
        } finally {
            MDC.clear();
            release.countDown();
            service.close();
        }
        assertThat(count(SUCCESS)).isEqualTo(65);

        // when & then
        service.alert(CATEGORY, ERROR_CODE, failureAt(100));
        assertThat(count(REJECTED)).isEqualTo(2);
    }

    @Test
    @DisplayName("큐 거부는 재알림 슬롯을 소비하지 않아 다음 발생이 재접수된다")
    void rejectedSubmissionReleasesCooldownReservation() {
        // given
        ExecutorService executor = mock(ExecutorService.class);
        SlackWebhookClient client = mock(SlackWebhookClient.class);
        doThrow(new RejectedExecutionException()).doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(executor).execute(any(Runnable.class));
        SlackAlertService service = new SlackAlertService(PROPERTIES, client, registry, () -> 0, executor);
        RuntimeException failure = failureAt(10);

        // when
        service.alert(CATEGORY, ERROR_CODE, failure);
        service.alert(CATEGORY, ERROR_CODE, failure);

        // then
        verify(client).send(anyString());
        assertThat(count(REJECTED)).isEqualTo(1);
        assertThat(count(SUCCESS)).isEqualTo(1);
        assertThat(count("suppressed")).isZero();
    }

    @Test
    @DisplayName("종료 대기 한도를 넘으면 대기 작업을 거부로 계수하고 강제 종료한다")
    void countsAbandonedQueuedWorkOnBoundedShutdown() throws InterruptedException {
        // given
        ExecutorService executor = mock(ExecutorService.class);
        List<Runnable> queued = new ArrayList<>();
        doAnswer(invocation -> {
            queued.add(invocation.getArgument(0, Runnable.class));
            return null;
        }).when(executor).execute(any(Runnable.class));
        when(executor.awaitTermination(5, TimeUnit.SECONDS)).thenReturn(false);
        when(executor.shutdownNow()).thenReturn(queued);
        SlackAlertService service = new SlackAlertService(PROPERTIES, mock(SlackWebhookClient.class),
                registry, () -> 0, executor);
        service.alert(CATEGORY, ERROR_CODE, failureAt(10));
        service.alert(CATEGORY, ERROR_CODE, failureAt(20));

        // when
        service.close();

        // then
        verify(executor).shutdown();
        verify(executor).awaitTermination(5, TimeUnit.SECONDS);
        verify(executor).shutdownNow();
        assertThat(count(REJECTED)).isEqualTo(2);
        assertThat(count(SUCCESS)).isZero();
    }

    @Test
    @DisplayName("종료 대기 중 인터럽트가 오면 인터럽트를 보존하고 남은 작업을 취소한다")
    void preservesShutdownInterruption() throws InterruptedException {
        // given
        ExecutorService executor = mock(ExecutorService.class);
        when(executor.awaitTermination(5, TimeUnit.SECONDS)).thenThrow(new InterruptedException());
        when(executor.shutdownNow()).thenReturn(List.of());
        SlackAlertService service = new SlackAlertService(PROPERTIES, mock(SlackWebhookClient.class),
                registry, () -> 0, executor);

        // when
        try {
            service.close();

            // then
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            verify(executor).shutdownNow();
        } finally {
            Thread.interrupted();
        }
    }

    private double count(String outcome) {
        return registry.get("slack.alert.delivery").tags("category", CATEGORY.name(), "outcome", outcome)
                .counter().count();
    }

    private RuntimeException failureAt(int line) {
        RuntimeException failure = new IllegalStateException("private exception message");
        failure.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("bhoon.sugang_helper.Example", "run", "Example.java", line)
        });
        return failure;
    }
}
