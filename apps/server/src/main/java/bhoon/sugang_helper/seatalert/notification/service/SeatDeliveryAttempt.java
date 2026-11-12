package bhoon.sugang_helper.seatalert.notification.service;

import bhoon.sugang_helper.seatalert.notification.dto.provider.NotificationTarget;
import java.util.List;

record SeatDeliveryAttempt(String title, String body, List<TargetFailure> failures,
                           RuntimeException channelFailure, boolean missingWebTarget) {

    static SeatDeliveryAttempt skipped(boolean missingWebTarget) {
        return new SeatDeliveryAttempt(null, null, List.of(), null, missingWebTarget);
    }

    boolean sent() {
        return title != null && channelFailure == null;
    }

    record TargetFailure(NotificationTarget target, RuntimeException exception) {
    }
}
