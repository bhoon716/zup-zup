package bhoon.sugang_helper.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.admin.application.AdminAuditService;
import bhoon.sugang_helper.notification.domain.SeatNotificationDelivery;
import bhoon.sugang_helper.notification.domain.SeatNotificationDeliveryStatus;
import bhoon.sugang_helper.notification.domain.SeatNotificationOutbox;
import bhoon.sugang_helper.notification.infra.NotificationChannel;
import bhoon.sugang_helper.notification.infra.SeatNotificationDeliveryJpaRepository;
import bhoon.sugang_helper.notification.infra.SeatNotificationOutboxJpaRepository;
import bhoon.sugang_helper.user.domain.Role;
import bhoon.sugang_helper.user.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(AdminNotificationDeliveryService.class)
class AdminNotificationDeliveryReplayConcurrencyTest {

    @Autowired
    private AdminNotificationDeliveryService service;
    @Autowired
    private SeatNotificationOutboxJpaRepository outboxRepository;
    @Autowired
    private SeatNotificationDeliveryJpaRepository deliveryRepository;
    @MockitoBean
    private AdminAuditService adminAuditService;

    @Test
    void concurrentReplaysChangeOneDlqDeliveryAndWriteOneAuditLog() throws Exception {
        SeatNotificationOutbox outbox = outboxRepository.saveAndFlush(SeatNotificationOutbox.builder()
                .courseKey("course-key")
                .courseName("Course")
                .previousSeats(0)
                .currentSeats(1)
                .build());
        SeatNotificationDelivery delivery = SeatNotificationDelivery.builder()
                .outbox(outbox)
                .userId(1L)
                .channel(NotificationChannel.EMAIL)
                .build();
        LocalDateTime now = LocalDateTime.of(2026, 7, 13, 14, 0);
        delivery.claim(now.plusMinutes(1));
        delivery.markFailure("N001", 1, now, now.plusSeconds(1));
        Long deliveryId = deliveryRepository.saveAndFlush(delivery).getId();
        User admin = User.builder().id(7L).email("admin@example.com").role(Role.ADMIN).build();

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> replayTask(admin, deliveryId, ready, start));
            Future<Boolean> second = executor.submit(() -> replayTask(admin, deliveryId, ready, start));
            ready.await();
            start.countDown();

            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(true, false);
        } finally {
            executor.shutdownNow();
        }

        assertThat(deliveryRepository.findById(deliveryId)).get()
                .extracting(SeatNotificationDelivery::getStatus)
                .isEqualTo(SeatNotificationDeliveryStatus.PENDING);
        verify(adminAuditService, times(1)).recordDeliveryReplay(
                org.mockito.ArgumentMatchers.any(User.class),
                org.mockito.ArgumentMatchers.eq(deliveryId),
                org.mockito.ArgumentMatchers.eq(SeatNotificationDeliveryStatus.DLQ),
                org.mockito.ArgumentMatchers.eq(1));
    }

    private boolean replayTask(User admin, Long deliveryId, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        try {
            service.replay(admin, deliveryId, false);
            return true;
        } catch (bhoon.sugang_helper.common.error.CustomException ignored) {
            return false;
        }
    }
}
