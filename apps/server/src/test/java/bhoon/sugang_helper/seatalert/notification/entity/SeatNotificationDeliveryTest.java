package bhoon.sugang_helper.seatalert.notification.entity;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.enums.SeatNotificationDeliveryStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SeatNotificationDeliveryTest {

    @Test
    @DisplayName("재전송은 원래 멱등 키를 유지하고 재시도 횟수를 초기화한다")
    void replayKeepsTheOriginalDeliveryIdempotencyKeyAndResetsTheRetryBudget() {
        // when
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

        // then
        assertThat(originalKey).isNotBlank();
        assertThat(delivery.getIdempotencyKey()).isEqualTo(originalKey);
        assertThat(delivery.getStatus()).isEqualTo(SeatNotificationDeliveryStatus.PENDING);
        assertThat(delivery.getAttempts()).isZero();
        assertThat(delivery.getLastError()).isNull();
        assertThat(delivery.getDeadLetteredAt()).isNull();
        assertThat(delivery.getClaimToken()).isNull();
    }
}
