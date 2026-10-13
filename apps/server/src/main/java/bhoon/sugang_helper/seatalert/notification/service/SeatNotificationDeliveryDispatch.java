package bhoon.sugang_helper.seatalert.notification.service;

import bhoon.sugang_helper.seatalert.enums.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationOutbox;

/**
 * 외부 provider 호출에 필요한 detached delivery snapshot입니다.
 */
record SeatNotificationDeliveryDispatch(
        Long userId,
        NotificationChannel channel,
        SeatNotificationOutbox outbox,
        String idempotencyKey) {
}
