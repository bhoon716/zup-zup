package bhoon.sugang_helper.support.feedback.dto;

import bhoon.sugang_helper.support.feedback.enums.FeedbackStatus;
import bhoon.sugang_helper.support.feedback.enums.FeedbackType;
import java.time.LocalDateTime;

public record AdminFeedbackDetail(
        Long id,
        FeedbackType type,
        String title,
        String content,
        FeedbackStatus status,
        LocalDateTime createdAt,
        boolean deleted,
        LocalDateTime deletedAt,
        Long authorId) {
}
