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
        // given
        SeatNotificationDeliveryClaim first = claim(1L, NotificationChannel.EMAIL);
        SeatNotificationDeliveryClaim second = claim(2L, NotificationChannel.WEB);
        when(processor.claimReadyDeliveries(1)).thenReturn(List.of(first), List.of(second));
        Thread caller = Thread.currentThread();
        doAnswer(invocation -> {
            assertThat(Thread.currentThread()).isSameAs(caller);
            return null;
        }).when(processor).processDelivery(any());

        // when
        worker.processPendingNotifications();

        // then
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
        // given
        SeatNotificationDeliveryClaim first = claim(1L, NotificationChannel.EMAIL);
        SeatNotificationDeliveryClaim second = claim(2L, NotificationChannel.WEB);
        when(processor.claimReadyDeliveries(1)).thenReturn(List.of(first), List.of(second));
        doThrow(new IllegalStateException("settlement unavailable")).when(processor).processDelivery(first);

        // when
        worker.processPendingNotifications();

        // then
        verify(processor).processDelivery(first);
        verify(processor).processDelivery(second);
        assertThat(meterRegistry.get("notification.outbox.queue.depth").tag("channel", "EMAIL").gauge().value())
                .isZero();
    }

    @Test
    void skipsOverlappingPollWhileThePreviousDeliveryIsStillRunning() throws Exception {
        // given
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

            // when
            worker.processPendingNotifications();

            // then
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
        // given
        SeatNotificationDeliveryClaim first = claim(1L, NotificationChannel.EMAIL);
        when(processor.claimReadyDeliveries(1)).thenReturn(List.of(first));
        doAnswer(invocation -> {
            worker.shutdown();
            return null;
        }).when(processor).processDelivery(first);

        // when
        worker.processPendingNotifications();
        worker.processPendingNotifications();

        // then
        verify(processor).materializePendingOutboxes();
        verify(processor).claimReadyDeliveries(1);
        verify(processor).processDelivery(first);
        verifyNoMoreInteractions(processor);
    }

    @Test
    void stoppedWorkerDoesNotClaimOrMaterialize() {
        // given
        worker.shutdown();

        // when
        worker.processPendingNotifications();

        // then
        verifyNoInteractions(processor);
    }

    @Test
    void materializationFailureDoesNotPermanentlyBlockLaterPolls() {
        // given
        doThrow(new IllegalStateException("database unavailable")).doNothing()
                .when(processor).materializePendingOutboxes();
        when(processor.claimReadyDeliveries(1)).thenReturn(List.of());

        // when & then
        assertThatThrownBy(worker::processPendingNotifications).isInstanceOf(IllegalStateException.class);
        worker.processPendingNotifications();
        verify(processor, times(2)).materializePendingOutboxes();
        verify(processor).claimReadyDeliveries(1);
    }

    @Test
    void interruptedCallerDoesNotTakeMoreWork() {
        // given
        Thread.currentThread().interrupt();
        try {
            // when
            worker.processPendingNotifications();

            // then
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
