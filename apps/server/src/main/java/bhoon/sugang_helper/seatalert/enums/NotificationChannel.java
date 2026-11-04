package bhoon.sugang_helper.seatalert.enums;

import java.util.List;

public enum NotificationChannel {
    EMAIL,
    // 과거 DB·API 이력을 읽기 위해 유지하며 신규 발송은 지원하지 않는다.
    FCM,
    WEB,
    DISCORD;

    public static List<NotificationChannel> supportedChannels() {
        return List.of(EMAIL, WEB, DISCORD);
    }
}
