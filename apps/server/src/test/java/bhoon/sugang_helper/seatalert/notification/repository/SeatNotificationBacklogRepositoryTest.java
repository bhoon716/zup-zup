package bhoon.sugang_helper.seatalert.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationDelivery;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationOutbox;
import bhoon.sugang_helper.seatalert.notification.enums.SeatNotificationDeliveryStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class SeatNotificationBacklogRepositoryTest {

    @Autowired
    private SeatNotificationOutboxRepository outboxRepository;

    @Autowired
    private SeatNotificationDeliveryRepository deliveryRepository;

    @Test
    @DisplayName("대기열 집계는 만료된 선점을 포함하고 미래 재시도·활성 선점·완료·DLQ를 제외한다")
    void countsOnlyReadyDeliveriesAndExpiredClaims() {
        // given
        LocalDateTime now = LocalDateTime.now().withNano(0);
        LocalDateTime readyAt = now.minusMinutes(20);
        SeatNotificationOutbox outbox = outboxRepository.saveAndFlush(SeatNotificationOutbox.builder()
                .courseKey("backlog-test").courseName("집계 테스트").previousSeats(0).currentSeats(1).build());
        SeatNotificationDelivery ready = delivery(outbox, 1L, NotificationChannel.EMAIL);
        ready.replay(readyAt);
        SeatNotificationDelivery expired = delivery(outbox, 2L, NotificationChannel.EMAIL);
        expired.replay(now.minusMinutes(2));
        expired.claim(now.minusSeconds(1));
        SeatNotificationDelivery future = delivery(outbox, 3L, NotificationChannel.EMAIL);
        future.replay(now.plusHours(1));
        SeatNotificationDelivery active = delivery(outbox, 4L, NotificationChannel.EMAIL);
        active.claim(now.plusMinutes(1));
        SeatNotificationDelivery sent = delivery(outbox, 5L, NotificationChannel.EMAIL);
        sent.markSent();
        SeatNotificationDelivery dead = delivery(outbox, 6L, NotificationChannel.EMAIL);
        dead.markFailure("UNEXPECTED", 1, now, now);
        SeatNotificationDelivery web = delivery(outbox, 7L, NotificationChannel.WEB);
        web.replay(now);
        deliveryRepository.saveAllAndFlush(List.of(ready, expired, future, active, sent, dead, web));

        // when
        var backlogs = deliveryRepository.findReadyBacklog(SeatNotificationDeliveryStatus.PENDING,
                SeatNotificationDeliveryStatus.PROCESSING, now);

        // then
        assertThat(backlogs).hasSize(2).anySatisfy(backlog -> {
            assertThat(backlog.getChannel()).isEqualTo(NotificationChannel.EMAIL);
            assertThat(backlog.getDepth()).isEqualTo(2);
            assertThat(backlog.getOldestReadyAt()).isEqualTo(readyAt);
        }).anySatisfy(backlog -> {
            assertThat(backlog.getChannel()).isEqualTo(NotificationChannel.WEB);
            assertThat(backlog.getDepth()).isEqualTo(1);
        });
    }

    private SeatNotificationDelivery delivery(SeatNotificationOutbox outbox, Long userId, NotificationChannel channel) {
        return SeatNotificationDelivery.builder().outbox(outbox).userId(userId).channel(channel).build();
    }
}
