package bhoon.sugang_helper.seatalert.notification.enums;

import java.util.List;

public enum SeatNotificationDeliveryStatus {
    PENDING,
    PROCESSING,
    SENT,
    DLQ;

    public static final List<SeatNotificationDeliveryStatus> ACTIVE_STATUSES = List.of(PENDING, PROCESSING);
}
