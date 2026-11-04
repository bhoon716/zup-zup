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

import bhoon.sugang_helper.seatalert.enums.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.dto.result.SeatNotificationDeliveryClaim;
import bhoon.sugang_helper.seatalert.notification.service.SeatNotificationOutboxProcessor;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SeatNotificationOutboxWorkerTest {

    @Mock
    private SeatNotificationOutboxProcessor processor;

    private SimpleMeterRegistry meterRegistry;
    private SeatNotificationOutboxWorker worker;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        worker = new SeatNotificationOutboxWorker(processor, meterRegistry, 2);
    }

    @AfterEach
    void tearDown() {
        worker.shutdown();
        meterRegistry.close();
    }

    @Test
    void claimsOnlyBoundedCapacityAndProcessesEveryClaimWithoutLoss() {

        SeatNotificationDeliveryClaim first = claim(1L, NotificationChannel.EMAIL);
        SeatNotificationDeliveryClaim second = claim(2L, NotificationChannel.WEB);
        when(processor.claimReadyDeliveries(1)).thenReturn(List.of(first), List.of(second));
        Thread caller = Thread.currentThread();
        doAnswer(invocation -> {
            assertThat(Thread.currentThread()).isSameAs(caller);
            return null;
        }).when(processor).processDelivery(any());


        worker.processPendingNotifications();


        var ordered = inOrder(processor);
        ordered.verify(processor).materializePendingOutboxes();
        ordered.verify(processor).claimReadyDeliveries(1);
        ordered.verify(processor).processDelivery(first);
        ordered.verify(processor).claimReadyDeliveries(1);
        ordered.verify(processor).processDelivery(second);
        verifyNoMoreInteractions(processor);
        assertThat(meterRegistry.get("notification.outbox.queue.depth").tag("channel", "EMAIL").gauge().value())
                .isZero();
    }

    @Test
    void oneDeliveryFailureDoesNotSkipTheNextChannelOrLeaveTheGaugeBusy() {

        SeatNotificationDeliveryClaim first = claim(1L, NotificationChannel.EMAIL);
        SeatNotificationDeliveryClaim second = claim(2L, NotificationChannel.WEB);
        when(processor.claimReadyDeliveries(1)).thenReturn(List.of(first), List.of(second));
        doThrow(new IllegalStateException("cookie=private-settlement-secret")).when(processor).processDelivery(first);
        Logger logger = (Logger) LoggerFactory.getLogger(SeatNotificationOutboxWorker.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            worker.processPendingNotifications();
            assertThat(appender.list).anySatisfy(event -> {
                assertThat(event.getFormattedMessage()).contains("발송 작업 처리 실패", "deliveryId=1",
                        "exceptionType=IllegalStateException");
                assertThat(ThrowableProxyUtil.asString(event.getThrowableProxy()))
                        .contains("oneDeliveryFailureDoesNotSkipTheNextChannelOrLeaveTheGaugeBusy")
                        .doesNotContain("private-settlement-secret");
            });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }


        verify(processor).processDelivery(first);
        verify(processor).processDelivery(second);
        assertThat(meterRegistry.get("notification.outbox.queue.depth").tag("channel", "EMAIL").gauge().value())
                .isZero();
    }

    @Test
    void skipsOverlappingPollWhileThePreviousDeliveryIsStillRunning() throws Exception {

        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        SeatNotificationDeliveryClaim first = claim(1L, NotificationChannel.EMAIL);
        when(processor.claimReadyDeliveries(1)).thenReturn(List.of(first), List.of());
        doAnswer(invocation -> {
            started.countDown();
            assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
            return null;
        }).when(processor).processDelivery(first);
        ExecutorService caller = Executors.newSingleThreadExecutor();
        try {
            Future<?> runningPoll = caller.submit(worker::processPendingNotifications);
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();


            worker.processPendingNotifications();


            verify(processor).materializePendingOutboxes();
            verify(processor).claimReadyDeliveries(1);
            release.countDown();
            runningPoll.get(5, TimeUnit.SECONDS);
            verify(processor).processDelivery(first);
        } finally {
            release.countDown();
            caller.shutdownNow();
        }
    }

    @Test
    void shutdownFinishesTheCurrentDeliveryWithoutClaimingAnotherOne() {

        SeatNotificationDeliveryClaim first = claim(1L, NotificationChannel.EMAIL);
        when(processor.claimReadyDeliveries(1)).thenReturn(List.of(first));
        doAnswer(invocation -> {
            worker.shutdown();
            return null;
        }).when(processor).processDelivery(first);


        worker.processPendingNotifications();
        worker.processPendingNotifications();


        verify(processor).materializePendingOutboxes();
        verify(processor).claimReadyDeliveries(1);
        verify(processor).processDelivery(first);
        verifyNoMoreInteractions(processor);
    }

    @Test
    void stoppedWorkerDoesNotClaimOrMaterialize() {

        worker.shutdown();


        worker.processPendingNotifications();


        verifyNoInteractions(processor);
    }

    @Test
    void materializationFailureDoesNotPermanentlyBlockLaterPolls() {

        doThrow(new IllegalStateException("database unavailable")).doNothing()
                .when(processor).materializePendingOutboxes();
        when(processor.claimReadyDeliveries(1)).thenReturn(List.of());


        assertThatThrownBy(worker::processPendingNotifications).isInstanceOf(IllegalStateException.class);
        worker.processPendingNotifications();
        verify(processor, times(2)).materializePendingOutboxes();
        verify(processor).claimReadyDeliveries(1);
    }

    @Test
    void interruptedCallerDoesNotTakeMoreWork() {

        Thread.currentThread().interrupt();
        try {

            worker.processPendingNotifications();


            verify(processor, never()).claimReadyDeliveries(1);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    private SeatNotificationDeliveryClaim claim(Long id, NotificationChannel channel) {
        return new SeatNotificationDeliveryClaim(id, "claim-" + id, LocalDateTime.now(), channel);
    }
}
