package bhoon.sugang_helper.notification.infra;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.notification.domain.SeatNotificationDelivery;
import bhoon.sugang_helper.notification.domain.SeatNotificationDeliveryStatus;
import bhoon.sugang_helper.notification.domain.SeatNotificationOutbox;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SeatNotificationDeliveryClaimTest {

    @Autowired
    private SeatNotificationOutboxJpaRepository outboxRepository;
    @Autowired
    private SeatNotificationDeliveryJpaRepository deliveryRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void competingWorkersClaimTheSameDeliveryOnlyOnce() throws Exception {
        TransactionTemplate setupTransaction = new TransactionTemplate(transactionManager);
        setupTransaction.executeWithoutResult(status -> {
            SeatNotificationOutbox outbox = outboxRepository.save(SeatNotificationOutbox.builder()
                    .courseKey("course-key")
                    .courseName("Course")
                    .previousSeats(0)
                    .currentSeats(1)
                    .build());
            deliveryRepository.save(SeatNotificationDelivery.builder()
                    .outbox(outbox)
                    .userId(1L)
                    .channel(NotificationChannel.EMAIL)
                    .build());
        });

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(claimTask(ready, start));
            Future<Boolean> second = executor.submit(claimTask(ready, start));
            ready.await();
            start.countDown();

            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(true, false);
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<Boolean> claimTask(CountDownLatch ready, CountDownLatch start) {
        return () -> {
            TransactionTemplate transaction = new TransactionTemplate(transactionManager);
            ready.countDown();
            start.await();
            return transaction.execute(status -> {
                List<SeatNotificationDelivery> deliveries = deliveryRepository.findReadyForUpdate(
                        SeatNotificationDeliveryStatus.PENDING,
                        SeatNotificationDeliveryStatus.PROCESSING,
                        LocalDateTime.now(),
                        PageRequest.of(0, 1));
                if (deliveries.isEmpty()) {
                    return false;
                }
                deliveries.getFirst().claim(LocalDateTime.now().plusMinutes(1));
                return true;
            });
        };
    }
}
