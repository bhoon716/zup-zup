package bhoon.sugang_helper.notification.application;

/**
 * DB lease를 소유한 worker만 delivery 상태를 확정하도록 하는 fencing token입니다.
 */
public record SeatNotificationDeliveryClaim(Long deliveryId, String claimToken) {
}
