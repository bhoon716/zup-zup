package bhoon.sugang_helper.seatalert.notification.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.dto.result.SeatNotificationDeliveryClaim;
import bhoon.sugang_helper.seatalert.notification.service.SeatNotificationDeliveryStateService;
import bhoon.sugang_helper.seatalert.notification.service.SeatNotificationOutboxProcessor;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class SeatNotificationOutboxWorkerTest {

    private static final String IN_FLIGHT_METRIC = "notification.outbox.in.flight";
    private static final String CHANNEL_TAG = "channel";
    private static final String SETTLEMENT_SECRET = "private-settlement-secret";

    @Mock
    private SeatNotificationOutboxProcessor processor;

    @Mock
    private SeatNotificationDeliveryStateService deliveryStateService;

    private SimpleMeterRegistry meterRegistry;
    private SeatNotificationOutboxWorker worker;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        worker = new SeatNotificationOutboxWorker(processor, deliveryStateService, meterRegistry, 2);
    }

    @AfterEach
    void tearDown() {
        worker.shutdown();
        meterRegistry.close();
    }

    @Test
    @DisplayName("작업 용량 이내만 선점하고 모든 선점 작업을 빠짐없이 처리한다")
    void claimsOnlyBoundedCapacityAndProcessesEveryClaimWithoutLoss() {
        // given
        SeatNotificationDeliveryClaim first = claim(1L, NotificationChannel.EMAIL);
        SeatNotificationDeliveryClaim second = claim(2L, NotificationChannel.WEB);
        when(deliveryStateService.claimReadyDeliveries(1)).thenReturn(List.of(first), List.of(second));
        Thread caller = Thread.currentThread();

        doAnswer(invocation -> {
            assertThat(Thread.currentThread()).isSameAs(caller);
            return null;
        }).when(processor).processDelivery(any());

        // when
        worker.processPendingNotifications();

        // then
        var ordered = inOrder(processor, deliveryStateService);
        ordered.verify(processor).materializePendingOutboxes();
        ordered.verify(deliveryStateService).claimReadyDeliveries(1);
        ordered.verify(processor).processDelivery(first);
        ordered.verify(deliveryStateService).claimReadyDeliveries(1);
        ordered.verify(processor).processDelivery(second);
        verify(deliveryStateService, times(2)).claimReadyDeliveries(1);
        verifyNoMoreInteractions(processor, deliveryStateService);
        assertThat(meterRegistry.get(IN_FLIGHT_METRIC).tag(CHANNEL_TAG, NotificationChannel.EMAIL.name()).gauge().value())
                .isZero();
    }

    @Test
    @DisplayName("발송 하나의 실패로 다음 채널을 건너뛰거나 실행 지표가 남지 않는다")
    void oneDeliveryFailureDoesNotSkipTheNextChannelOrLeaveTheGaugeBusy() {
        // given
        SeatNotificationDeliveryClaim first = claim(1L, NotificationChannel.EMAIL);
        SeatNotificationDeliveryClaim second = claim(2L, NotificationChannel.WEB);
        when(deliveryStateService.claimReadyDeliveries(1)).thenReturn(List.of(first), List.of(second));
        doThrow(new IllegalStateException("cookie=" + SETTLEMENT_SECRET)).when(processor).processDelivery(first);
        Logger logger = (Logger) LoggerFactory.getLogger(SeatNotificationOutboxWorker.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            // when
            worker.processPendingNotifications();

            // then
            assertThat(appender.list).anySatisfy(event -> {
                assertThat(event.getFormattedMessage()).contains("발송 작업 처리 실패", "deliveryId=1",
                        "exceptionType=IllegalStateException");
                assertThat(ThrowableProxyUtil.asString(event.getThrowableProxy()))
                        .contains("oneDeliveryFailureDoesNotSkipTheNextChannelOrLeaveTheGaugeBusy")
                        .doesNotContain(SETTLEMENT_SECRET);
            });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        verify(processor).processDelivery(first);
        verify(processor).processDelivery(second);
        assertThat(meterRegistry.get(IN_FLIGHT_METRIC).tag(CHANNEL_TAG, NotificationChannel.EMAIL.name()).gauge().value())
                .isZero();
    }

    @Test
    @DisplayName("이전 발송 실행 중에는 겹치는 폴링을 생략한다")
    void skipsOverlappingPollWhileThePreviousDeliveryIsStillRunning() throws Exception {
        // given
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        SeatNotificationDeliveryClaim first = claim(1L, NotificationChannel.EMAIL);
        when(deliveryStateService.claimReadyDeliveries(1)).thenReturn(List.of(first), List.of());

        doAnswer(invocation -> {
            started.countDown();
            assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
            return null;
        }).when(processor).processDelivery(first);
        ExecutorService caller = Executors.newSingleThreadExecutor();
        try {
            // when
            Future<?> runningPoll = caller.submit(worker::processPendingNotifications);
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();

            worker.processPendingNotifications();

            // then
            verify(processor).materializePendingOutboxes();
            verify(deliveryStateService).claimReadyDeliveries(1);
            release.countDown();
            runningPoll.get(5, TimeUnit.SECONDS);
            verify(processor).processDelivery(first);
        } finally {
            release.countDown();
            caller.shutdownNow();
        }
    }

    @Test
    @DisplayName("종료 시 현재 발송은 마치고 새 작업은 선점하지 않는다")
    void shutdownFinishesTheCurrentDeliveryWithoutClaimingAnotherOne() {
        // given
        SeatNotificationDeliveryClaim first = claim(1L, NotificationChannel.EMAIL);
        when(deliveryStateService.claimReadyDeliveries(1)).thenReturn(List.of(first));
        doAnswer(invocation -> {
            worker.shutdown();
            return null;
        }).when(processor).processDelivery(first);

        // when
        worker.processPendingNotifications();
        worker.processPendingNotifications();

        // then
        verify(processor).materializePendingOutboxes();
        verify(deliveryStateService).claimReadyDeliveries(1);
        verify(processor).processDelivery(first);
        verifyNoMoreInteractions(processor, deliveryStateService);
    }

    @Test
    @DisplayName("종료된 작업자는 선점이나 발송 작업 생성을 하지 않는다")
    void stoppedWorkerDoesNotClaimOrMaterialize() {
        // given
        worker.shutdown();

        // when
        worker.processPendingNotifications();

        // then
        verifyNoInteractions(processor, deliveryStateService);
    }

    @Test
    @DisplayName("발송 작업 생성 실패가 이후 폴링을 계속 막지 않는다")
    void materializationFailureDoesNotPermanentlyBlockLaterPolls() {
        // given
        doThrow(new IllegalStateException("database unavailable")).doNothing()
                .when(processor).materializePendingOutboxes();
        when(deliveryStateService.claimReadyDeliveries(1)).thenReturn(List.of());

        // when & then
        assertThatThrownBy(worker::processPendingNotifications).isInstanceOf(IllegalStateException.class);

        // when
        worker.processPendingNotifications();

        // then
        verify(processor, times(2)).materializePendingOutboxes();
        verify(deliveryStateService).claimReadyDeliveries(1);
    }

    @Test
    @DisplayName("인터럽트된 호출자는 추가 작업을 받지 않는다")
    void interruptedCallerDoesNotTakeMoreWork() {
        // given
        Thread.currentThread().interrupt();

        try {
            // when
            worker.processPendingNotifications();

            // then
            verify(deliveryStateService, never()).claimReadyDeliveries(1);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    private SeatNotificationDeliveryClaim claim(Long id, NotificationChannel channel) {
        return new SeatNotificationDeliveryClaim(id, "claim-" + id, LocalDateTime.now(), channel);
    }
}
