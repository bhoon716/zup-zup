package bhoon.sugang_helper.seatalert.notification.service;




record SeatNotificationDeliveryFailureResult(boolean settled, boolean deadLettered, int attempts) {

    static SeatNotificationDeliveryFailureResult stale() {
        return new SeatNotificationDeliveryFailureResult(false, false, 0);
    }
}
