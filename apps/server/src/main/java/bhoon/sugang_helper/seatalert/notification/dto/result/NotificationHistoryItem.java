package bhoon.sugang_helper.seatalert.notification.dto.result;

import bhoon.sugang_helper.seatalert.enums.NotificationChannel;
import java.time.LocalDateTime;

public record NotificationHistoryItem(
        Long id,
        Long userId,
        String courseKey,
        String title,
        String message,
        NotificationChannel channel,
        LocalDateTime createdAt) {
}
