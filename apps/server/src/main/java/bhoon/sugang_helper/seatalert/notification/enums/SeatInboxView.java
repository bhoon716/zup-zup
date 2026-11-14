package bhoon.sugang_helper.seatalert.notification.enums;

import static bhoon.sugang_helper.seatalert.notification.enums.InboxNotificationType.ANNOUNCEMENT;
import static bhoon.sugang_helper.seatalert.notification.enums.InboxNotificationType.DELIVERY_FAILED;
import static bhoon.sugang_helper.seatalert.notification.enums.InboxNotificationType.DEVICE_REREGISTRATION;
import static bhoon.sugang_helper.seatalert.notification.enums.InboxNotificationType.FEEDBACK_REPLY;
import static bhoon.sugang_helper.seatalert.notification.enums.InboxNotificationType.SEAT_OPENED;

import java.util.Set;

public enum SeatInboxView {
    INBOX(Set.of(FEEDBACK_REPLY, DELIVERY_FAILED, DEVICE_REREGISTRATION)),
    SEAT_ALERTS(Set.of(SEAT_OPENED, ANNOUNCEMENT));

    private final Set<InboxNotificationType> types;

    SeatInboxView(Set<InboxNotificationType> types) {
        this.types = types;
    }

    public Set<InboxNotificationType> types() {
        return types;
    }
}
