package bhoon.sugang_helper.notification.application;

/**
 * 오래된 worker의 결과가 현재 lease를 덮어쓰지 않았는지 함께 나타냅니다.
 */
record SeatNotificationDeliveryFailureResult(boolean settled, boolean deadLettered, int attempts) {

    static SeatNotificationDeliveryFailureResult stale() {
        return new SeatNotificationDeliveryFailureResult(false, false, 0);
    }
}
