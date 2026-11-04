package bhoon.sugang_helper.seatalert.notification.dto.response;

import bhoon.sugang_helper.seatalert.enums.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.dto.result.NotificationHistoryItem;
import bhoon.sugang_helper.seatalert.notification.entity.NotificationHistory;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationHistoryResponse {
    private Long id;

    private String courseKey;

    private String title;

    private String message;

    private NotificationChannel channel;

    private LocalDateTime sentAt;

    public static NotificationHistoryResponse from(NotificationHistory history) {
        return NotificationHistoryResponse.builder()
                .id(history.getId())
                .courseKey(history.getCourseKey())
                .title(history.getTitle())
                .message(history.getMessage())
                .channel(history.getChannel())
                .sentAt(history.getCreatedAt())
                .build();
    }

    public static NotificationHistoryResponse from(NotificationHistoryItem history) {
        return NotificationHistoryResponse.builder()
                .id(history.id())
                .courseKey(history.courseKey())
                .title(history.title())
                .message(history.message())
                .channel(history.channel())
                .sentAt(history.createdAt())
                .build();
    }
}
