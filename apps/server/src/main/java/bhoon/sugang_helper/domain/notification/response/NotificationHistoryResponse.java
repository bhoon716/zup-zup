package bhoon.sugang_helper.domain.notification.response;

import bhoon.sugang_helper.domain.notification.entity.NotificationHistory;
import bhoon.sugang_helper.domain.notification.sender.NotificationChannel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

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
}
