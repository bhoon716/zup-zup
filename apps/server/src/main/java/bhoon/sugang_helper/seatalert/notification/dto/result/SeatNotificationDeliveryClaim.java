package bhoon.sugang_helper.seatalert.notification.dto.result;

import bhoon.sugang_helper.seatalert.enums.NotificationChannel;
import java.time.LocalDateTime;




public record SeatNotificationDeliveryClaim(Long deliveryId, String claimToken, LocalDateTime claimedAt,
                                            NotificationChannel channel) {

    public SeatNotificationDeliveryClaim(Long deliveryId, String claimToken) {
        this(deliveryId, claimToken, LocalDateTime.now(), null);
    }

    public SeatNotificationDeliveryClaim(Long deliveryId, String claimToken, LocalDateTime claimedAt) {
        this(deliveryId, claimToken, claimedAt, null);
    }
}
