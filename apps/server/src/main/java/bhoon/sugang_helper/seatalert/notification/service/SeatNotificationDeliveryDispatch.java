package bhoon.sugang_helper.seatalert.notification.service;

import bhoon.sugang_helper.seatalert.enums.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationOutbox;




record SeatNotificationDeliveryDispatch(
        Long userId,
        NotificationChannel channel,
        SeatNotificationOutbox outbox,
        String idempotencyKey) {
}
