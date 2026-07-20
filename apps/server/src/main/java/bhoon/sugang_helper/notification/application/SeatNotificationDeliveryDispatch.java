package bhoon.sugang_helper.notification.application;

import bhoon.sugang_helper.notification.domain.SeatNotificationOutbox;
import bhoon.sugang_helper.notification.infra.NotificationChannel;

/**
 * 외부 provider 호출에 필요한 detached delivery snapshot입니다.
 */
record SeatNotificationDeliveryDispatch(
        Long userId,
        NotificationChannel channel,
        SeatNotificationOutbox outbox,
        String idempotencyKey) {
}
