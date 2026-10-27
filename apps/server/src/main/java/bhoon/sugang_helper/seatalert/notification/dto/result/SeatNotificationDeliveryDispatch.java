package bhoon.sugang_helper.seatalert.notification.dto.result;

import bhoon.sugang_helper.seatalert.enums.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationOutbox;




public record SeatNotificationDeliveryDispatch(
        Long userId,
        NotificationChannel channel,
        SeatNotificationOutbox outbox,
        String idempotencyKey) {
}
