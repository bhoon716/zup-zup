package bhoon.sugang_helper.seatalert.api;

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
