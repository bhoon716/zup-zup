package bhoon.sugang_helper.notification.application;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.notification.domain.SeatNotificationDelivery;
import bhoon.sugang_helper.notification.domain.SeatNotificationOutbox;
import bhoon.sugang_helper.notification.infra.NotificationChannel;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class AdminNotificationDeliveryResponseTest {

    @Test
    void mapsKnownNotificationErrorCodeToItsHumanReadableMessage() {
        SeatNotificationDelivery delivery = deliveryWithFailure("N012");

        AdminNotificationDeliveryResponse response = AdminNotificationDeliveryResponse.from(delivery);

        assertThat(response.lastError()).isEqualTo("N012");
        assertThat(response.lastErrorReason()).isEqualTo("유효하지 않은 Web Push 구독 정보입니다. (재구독 필요)");
    }

    @Test
    void mapsDefinedNonNotificationErrorCodeToItsHumanReadableMessage() {
        SeatNotificationDelivery delivery = deliveryWithFailure("G002");

        AdminNotificationDeliveryResponse response = AdminNotificationDeliveryResponse.from(delivery);

        assertThat(response.lastError()).isEqualTo("G002");
        assertThat(response.lastErrorReason()).isEqualTo("잘못된 입력값입니다.");
    }

    @Test
    void usesSafeFallbackForAnUndefinedNotificationErrorCode() {
        SeatNotificationDelivery delivery = deliveryWithFailure("N999");

        AdminNotificationDeliveryResponse response = AdminNotificationDeliveryResponse.from(delivery);

        assertThat(response.lastError()).isEqualTo("N999");
        assertThat(response.lastErrorReason()).isEqualTo("기타 수신 실패");
    }

    @Test
    void leavesFailureReasonEmptyWhenDeliveryHasNoFailure() {
        AdminNotificationDeliveryResponse response = AdminNotificationDeliveryResponse.from(
                SeatNotificationDelivery.builder()
                        .outbox(outbox())
                        .userId(1L)
                        .channel(NotificationChannel.EMAIL)
                        .build());

        assertThat(response.lastErrorReason()).isNull();
    }

    private SeatNotificationDelivery deliveryWithFailure(String errorCode) {
        SeatNotificationDelivery delivery = SeatNotificationDelivery.builder()
                .outbox(outbox())
                .userId(1L)
                .channel(NotificationChannel.WEB)
                .build();
        delivery.claim(LocalDateTime.of(2026, 9, 6, 12, 0));
        delivery.markFailure(errorCode, 1, LocalDateTime.of(2026, 9, 6, 12, 0),
                LocalDateTime.of(2026, 9, 6, 12, 1));
        return delivery;
    }

    private SeatNotificationOutbox outbox() {
        return SeatNotificationOutbox.builder()
                .courseKey("course-key")
                .courseName("Course")
                .previousSeats(0)
                .currentSeats(1)
                .build();
    }
}
