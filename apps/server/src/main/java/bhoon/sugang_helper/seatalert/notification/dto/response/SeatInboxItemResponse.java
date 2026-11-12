package bhoon.sugang_helper.seatalert.notification.dto.response;

import static bhoon.sugang_helper.seatalert.notification.entity.SeatInboxItem.UNAVAILABLE_MESSAGE;
import static bhoon.sugang_helper.seatalert.notification.entity.SeatInboxItem.UNAVAILABLE_TITLE;

import bhoon.sugang_helper.seatalert.notification.entity.SeatInboxItem;
import bhoon.sugang_helper.seatalert.notification.enums.InboxNotificationType;
import java.time.LocalDateTime;

public record SeatInboxItemResponse(
        Long id,
        InboxNotificationType type,
        String courseKey,
        String courseName,
        Integer availableSeats,
        String title,
        String message,
        Long targetId,
        boolean available,
        LocalDateTime occurredAt,
        LocalDateTime readAt
) {

    public static SeatInboxItemResponse from(SeatInboxItem item) {
        String title = item.isAvailable() ? item.getTitle() : UNAVAILABLE_TITLE;
        String message = item.isAvailable() ? item.getMessage() : UNAVAILABLE_MESSAGE;
        return new SeatInboxItemResponse(
                item.getId(), item.getType(), item.getCourseKey(), item.getCourseName(), item.getAvailableSeats(),
                title, message, item.getTargetId(), item.isAvailable(), item.getOccurredAt(), item.getReadAt());
    }
}
