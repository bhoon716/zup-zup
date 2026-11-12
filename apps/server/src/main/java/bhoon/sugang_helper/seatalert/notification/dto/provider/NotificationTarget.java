package bhoon.sugang_helper.seatalert.notification.dto.provider;

import bhoon.sugang_helper.identity.api.notifications.NotificationDevice;
import lombok.Builder;
import lombok.Getter;

@Getter
public class NotificationTarget {
    private final String recipient;
    private final String p256dh;
    private final String auth;

    private final NotificationDevice device;

    @Builder
    private NotificationTarget(String recipient, String p256dh, String auth, NotificationDevice device) {
        this.recipient = recipient;
        this.p256dh = p256dh;
        this.auth = auth;
        this.device = device;
    }

    public static NotificationTarget of(String recipient) {
        return NotificationTarget.builder()
                .recipient(recipient)
                .build();
    }

    public static NotificationTarget ofWeb(NotificationDevice device) {
        return new NotificationTarget(device.token(), device.p256dh(), device.auth(), device);
    }

    public static NotificationTarget ofWeb(String endpoint, String p256dh, String auth) {
        return NotificationTarget.builder()
                .recipient(endpoint)
                .p256dh(p256dh)
                .auth(auth)
                .build();
    }
}
