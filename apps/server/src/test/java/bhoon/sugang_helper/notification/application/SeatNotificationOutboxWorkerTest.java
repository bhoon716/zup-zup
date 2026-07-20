package bhoon.sugang_helper.notification.application;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.notification.infra.NotificationChannel;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class SeatNotificationOutboxWorkerTest {

    @Mock
    private SeatNotificationOutboxProcessor processor;

    private SimpleMeterRegistry meterRegistry;
    private ThreadPoolExecutor executor;
    private SeatNotificationOutboxWorker worker;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1));
        worker = new SeatNotificationOutboxWorker(processor, meterRegistry, 1, 1, executor);
    }

    @AfterEach
    void tearDown() {
        worker.shutdown();
        meterRegistry.close();
    }

    @Test
    void claimsOnlyBoundedCapacityAndProcessesEveryClaimWithoutLoss() throws Exception {
        SeatNotificationDeliveryClaim first = new SeatNotificationDeliveryClaim(
                1L, "claim-1", java.time.LocalDateTime.now(), NotificationChannel.EMAIL);
        SeatNotificationDeliveryClaim second = new SeatNotificationDeliveryClaim(
                2L, "claim-2", java.time.LocalDateTime.now(), NotificationChannel.EMAIL);
        when(processor.claimReadyDeliveries(anyInt())).thenReturn(List.of(first, second));

        worker.processPendingNotifications();
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);

        verify(processor).claimReadyDeliveries(2);
        verify(processor).processDelivery(first);
        verify(processor).processDelivery(second);
    }
}
