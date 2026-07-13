package bhoon.sugang_helper.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.notification.infra.NotificationChannel;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class SeatNotificationDeliveryTest {

    @Test
    void replayKeepsTheOriginalDeliveryIdempotencyKeyAndResetsTheRetryBudget() {
        SeatNotificationDelivery delivery = SeatNotificationDelivery.builder()
                .outbox(SeatNotificationOutbox.builder()
                        .courseKey("course-key")
                        .courseName("Course")
                        .previousSeats(0)
                        .currentSeats(1)
                        .build())
                .userId(1L)
                .channel(NotificationChannel.EMAIL)
                .build();
        String originalKey = delivery.getIdempotencyKey();
        LocalDateTime now = LocalDateTime.of(2026, 7, 13, 14, 0);

        delivery.claim(now.plusMinutes(1));
        delivery.markFailure("N001", 1, now, now.plusSeconds(1));
        delivery.replay(now.plusMinutes(2));

        assertThat(originalKey).isNotBlank();
        assertThat(delivery.getIdempotencyKey()).isEqualTo(originalKey);
        assertThat(delivery.getStatus()).isEqualTo(SeatNotificationDeliveryStatus.PENDING);
        assertThat(delivery.getAttempts()).isZero();
        assertThat(delivery.getLastError()).isNull();
        assertThat(delivery.getDeadLetteredAt()).isNull();
        assertThat(delivery.getClaimToken()).isNull();
    }
}
