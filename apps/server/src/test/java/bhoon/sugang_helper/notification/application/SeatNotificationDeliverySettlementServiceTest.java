package bhoon.sugang_helper.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;

import bhoon.sugang_helper.notification.domain.SeatNotificationDelivery;
import bhoon.sugang_helper.notification.domain.SeatNotificationDeliveryStatus;
import bhoon.sugang_helper.notification.domain.SeatNotificationOutbox;
import bhoon.sugang_helper.notification.domain.SeatNotificationOutboxStatus;
import bhoon.sugang_helper.notification.infra.NotificationChannel;
import bhoon.sugang_helper.notification.infra.SeatNotificationDeliveryJpaRepository;
import bhoon.sugang_helper.notification.infra.SeatNotificationOutboxJpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(SeatNotificationDeliverySettlementService.class)
class SeatNotificationDeliverySettlementServiceTest {

    @Autowired
    private SeatNotificationDeliverySettlementService settlementService;
    @Autowired
    private SeatNotificationOutboxJpaRepository outboxRepository;
    @MockitoSpyBean
    private SeatNotificationDeliveryJpaRepository deliveryRepository;
    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void staleWorkerCannotMarkAReclaimedDeliveryAsSent() {
        SeatNotificationOutbox outbox = outboxRepository.saveAndFlush(SeatNotificationOutbox.builder()
                .courseKey("course-key")
                .courseName("Course")
                .previousSeats(0)
                .currentSeats(1)
                .build());
        SeatNotificationDelivery delivery = deliveryRepository.saveAndFlush(SeatNotificationDelivery.builder()
                .outbox(outbox)
                .userId(1L)
                .channel(NotificationChannel.EMAIL)
                .build());
        LocalDateTime now = LocalDateTime.of(2026, 7, 13, 14, 0);
        String firstClaimToken = delivery.claim(now.minusMinutes(1));
        delivery = deliveryRepository.saveAndFlush(delivery);
        String secondClaimToken = delivery.claim(now.plusMinutes(1));
        delivery = deliveryRepository.saveAndFlush(delivery);

        boolean settled = settlementService.markSent(new SeatNotificationDeliveryClaim(
                delivery.getId(), firstClaimToken));

        assertThat(settled).isFalse();
        assertThat(deliveryRepository.findById(delivery.getId())).get()
                .satisfies(current -> {
                    assertThat(current.getStatus()).isEqualTo(SeatNotificationDeliveryStatus.PROCESSING);
                    assertThat(current.getClaimToken()).isEqualTo(secondClaimToken);
                });
    }

    @Test
    void concurrentLastDeliveriesReconcileOutboxToCompleted() throws Exception {
        SeatNotificationOutbox outbox = outboxRepository.saveAndFlush(SeatNotificationOutbox.builder()
                .courseKey("course-key")
                .courseName("Course")
                .previousSeats(0)
                .currentSeats(1)
                .build());
        outbox.markProcessing();
        outbox = outboxRepository.saveAndFlush(outbox);

        SeatNotificationDelivery firstDelivery = claimedDelivery(outbox, 1L);
        SeatNotificationDelivery secondDelivery = claimedDelivery(outbox, 2L);
        deliveryRepository.saveAllAndFlush(List.of(firstDelivery, secondDelivery));

        CountDownLatch countInvocations = new CountDownLatch(2);
        doAnswer(invocation -> {
            Long outboxId = invocation.getArgument(0);
            List<SeatNotificationDeliveryStatus> statuses = invocation.getArgument(1);
            long result = entityManager.createQuery("""
                    select count(d) from SeatNotificationDelivery d
                    where d.outbox.id = :outboxId and d.status in :statuses
                    """, Long.class)
                    .setParameter("outboxId", outboxId)
                    .setParameter("statuses", statuses)
                    .getSingleResult();
            countInvocations.countDown();
            countInvocations.await(250, TimeUnit.MILLISECONDS);
            return result;
        }).when(deliveryRepository).countByOutboxIdAndStatusIn(anyLong(), any());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> settlementService.markSent(
                    new SeatNotificationDeliveryClaim(firstDelivery.getId(), firstDelivery.getClaimToken())));
            Future<Boolean> second = executor.submit(() -> settlementService.markSent(
                    new SeatNotificationDeliveryClaim(secondDelivery.getId(), secondDelivery.getClaimToken())));

            assertThat(first.get(10, TimeUnit.SECONDS)).isTrue();
            assertThat(second.get(10, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }

        assertThat(outboxRepository.findById(outbox.getId())).get()
                .extracting(SeatNotificationOutbox::getStatus)
                .isEqualTo(SeatNotificationOutboxStatus.COMPLETED);
    }

    private SeatNotificationDelivery claimedDelivery(SeatNotificationOutbox outbox, Long userId) {
        SeatNotificationDelivery delivery = SeatNotificationDelivery.builder()
                .outbox(outbox)
                .userId(userId)
                .channel(NotificationChannel.EMAIL)
                .build();
        delivery.claim(LocalDateTime.now().plusMinutes(1));
        return delivery;
    }
}
