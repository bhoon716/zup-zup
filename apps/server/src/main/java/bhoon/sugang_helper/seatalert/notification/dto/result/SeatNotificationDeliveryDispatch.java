package bhoon.sugang_helper.seatalert.notification.dto.result;

import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationDelivery;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationOutbox;

public record SeatNotificationDeliveryDispatch(
        Long userId,
        NotificationChannel channel,
        SeatNotificationOutbox outbox,
        String idempotencyKey) {

    public static SeatNotificationDeliveryDispatch from(SeatNotificationDelivery delivery) {
        return new SeatNotificationDeliveryDispatch(
                delivery.getUserId(), delivery.getChannel(), delivery.getOutbox(), delivery.getIdempotencyKey());
    }
}
