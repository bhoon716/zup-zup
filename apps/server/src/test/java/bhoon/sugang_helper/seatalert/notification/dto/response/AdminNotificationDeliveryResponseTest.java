package bhoon.sugang_helper.seatalert.notification.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationDelivery;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationOutbox;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdminNotificationDeliveryResponseTest {

    @Test
    @DisplayName("알려진 알림 오류 코드는 읽기 쉬운 메시지로 변환한다")
    void mapsKnownNotificationErrorCodeToItsHumanReadableMessage() {
        // given
        SeatNotificationDelivery delivery = deliveryWithFailure("N012");

        // when
        AdminNotificationDeliveryResponse response = AdminNotificationDeliveryResponse.from(delivery);

        // then
        assertThat(response.lastError()).isEqualTo("N012");
        assertThat(response.lastErrorReason()).isEqualTo("유효하지 않은 Web Push 구독 정보입니다. (재구독 필요)");
    }

    @Test
    @DisplayName("정의된 일반 오류 코드도 읽기 쉬운 메시지로 변환한다")
    void mapsDefinedNonNotificationErrorCodeToItsHumanReadableMessage() {
        // given
        SeatNotificationDelivery delivery = deliveryWithFailure("G002");

        // when
        AdminNotificationDeliveryResponse response = AdminNotificationDeliveryResponse.from(delivery);

        // then
        assertThat(response.lastError()).isEqualTo("G002");
        assertThat(response.lastErrorReason()).isEqualTo("잘못된 입력값입니다.");
    }

    @Test
    @DisplayName("정의되지 않은 알림 오류 코드에는 안전한 기본 메시지를 사용한다")
    void usesSafeFallbackForAnUndefinedNotificationErrorCode() {
        // given
        SeatNotificationDelivery delivery = deliveryWithFailure("N999");

        // when
        AdminNotificationDeliveryResponse response = AdminNotificationDeliveryResponse.from(delivery);

        // then
        assertThat(response.lastError()).isEqualTo("N999");
        assertThat(response.lastErrorReason()).isEqualTo("기타 수신 실패");
    }

    @Test
    @DisplayName("발송 실패가 없으면 실패 사유를 비워 둔다")
    void leavesFailureReasonEmptyWhenDeliveryHasNoFailure() {
        // when
        AdminNotificationDeliveryResponse response = AdminNotificationDeliveryResponse.from(
                SeatNotificationDelivery.builder()
                        .outbox(outbox())
                        .userId(1L)
                        .channel(NotificationChannel.EMAIL)
                        .build());

        // then
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
