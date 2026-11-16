package bhoon.sugang_helper.seatalert.notification.dto.response;

import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.api.NotificationHistoryItem;
import bhoon.sugang_helper.seatalert.notification.entity.NotificationHistory;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationHistoryResponse {
    private final Long id;

    private final String courseKey;

    private final String title;

    private final String message;

    private final NotificationChannel channel;

    private final LocalDateTime sentAt;

    public static NotificationHistoryResponse from(NotificationHistory history) {
        return new NotificationHistoryResponse(
                history.getId(),
                history.getCourseKey(),
                history.getTitle(),
                history.getMessage(),
                history.getChannel(),
                history.getCreatedAt());
    }

    public static NotificationHistoryResponse from(NotificationHistoryItem history) {
        return new NotificationHistoryResponse(
                history.id(),
                history.courseKey(),
                history.title(),
                history.message(),
                history.channel(),
                history.createdAt());
    }
}
