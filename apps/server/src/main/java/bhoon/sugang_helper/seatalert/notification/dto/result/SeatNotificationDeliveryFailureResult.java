package bhoon.sugang_helper.seatalert.notification.dto.result;




public record SeatNotificationDeliveryFailureResult(boolean settled, boolean deadLettered, int attempts) {

    public static SeatNotificationDeliveryFailureResult stale() {
        return new SeatNotificationDeliveryFailureResult(false, false, 0);
    }
}
