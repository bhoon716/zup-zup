package bhoon.sugang_helper.support.feedback.dto.result;

import bhoon.sugang_helper.support.feedback.enums.FeedbackStatus;
import bhoon.sugang_helper.support.feedback.enums.FeedbackType;
import java.time.LocalDateTime;

public record AdminFeedbackSummary(
        Long id,
        FeedbackType type,
        String title,
        FeedbackStatus status,
        LocalDateTime createdAt,
        boolean hasReplies,
        boolean deleted,
        LocalDateTime deletedAt,
        Long authorId) {
}
