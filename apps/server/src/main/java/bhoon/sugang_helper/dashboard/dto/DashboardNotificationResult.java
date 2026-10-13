package bhoon.sugang_helper.dashboard.dto;

import bhoon.sugang_helper.seatalert.enums.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.dto.NotificationHistoryItem;
import java.time.LocalDateTime;

public record DashboardNotificationResult(
        Long id,
        String courseKey,
        String title,
        String message,
        NotificationChannel channel,
        LocalDateTime sentAt) {

    public static DashboardNotificationResult from(NotificationHistoryItem item) {
        return new DashboardNotificationResult(
                item.id(), item.courseKey(), item.title(), item.message(), item.channel(), item.createdAt());
    }
}
