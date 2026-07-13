package bhoon.sugang_helper.notification.application;

import java.time.LocalDateTime;
import bhoon.sugang_helper.notification.infra.NotificationChannel;

/**
 * DB lease를 소유한 worker만 delivery 상태를 확정하도록 하는 fencing token입니다.
 */
public record SeatNotificationDeliveryClaim(Long deliveryId, String claimToken, LocalDateTime claimedAt,
                                            NotificationChannel channel) {

    public SeatNotificationDeliveryClaim(Long deliveryId, String claimToken) {
        this(deliveryId, claimToken, LocalDateTime.now(), null);
    }

    public SeatNotificationDeliveryClaim(Long deliveryId, String claimToken, LocalDateTime claimedAt) {
        this(deliveryId, claimToken, claimedAt, null);
    }
}
