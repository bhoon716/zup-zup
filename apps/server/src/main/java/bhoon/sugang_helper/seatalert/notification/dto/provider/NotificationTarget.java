package bhoon.sugang_helper.seatalert.notification.dto.provider;

import lombok.Builder;
import lombok.Getter;

@Getter
public class NotificationTarget {
    private final String recipient;
    private final String p256dh;
    private final String auth;

    @Builder
    private NotificationTarget(String recipient, String p256dh, String auth) {
        this.recipient = recipient;
        this.p256dh = p256dh;
        this.auth = auth;
    }

    public static NotificationTarget of(String recipient) {
        return NotificationTarget.builder()
                .recipient(recipient)
                .build();
    }

    public static NotificationTarget ofWeb(String endpoint, String p256dh, String auth) {
        return NotificationTarget.builder()
                .recipient(endpoint)
                .p256dh(p256dh)
                .auth(auth)
                .build();
    }
}
