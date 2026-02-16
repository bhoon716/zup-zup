package bhoon.sugang_helper.domain.notification.sender;

import lombok.Builder;
import lombok.Getter;

@Getter
public class NotificationTarget {
    private final String recipient; // 이메일, FCM 토큰, 웹푸시 엔드포인트
    private final String p256dh; // 웹푸시 전용 공개키
    private final String auth; // 웹푸시 전용 인증키

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
