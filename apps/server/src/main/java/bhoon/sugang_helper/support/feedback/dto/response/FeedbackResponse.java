package bhoon.sugang_helper.support.feedback.dto.response;

import bhoon.sugang_helper.support.feedback.dto.result.FeedbackListItem;
import bhoon.sugang_helper.support.feedback.enums.FeedbackStatus;
import bhoon.sugang_helper.support.feedback.enums.FeedbackType;
import java.time.LocalDateTime;

public record FeedbackResponse(
        Long id,
        FeedbackType type,
        String title,
        FeedbackStatus status,
        LocalDateTime createdAt,
        boolean hasReplies) {
    public static FeedbackResponse from(FeedbackListItem feedback) {
        return new FeedbackResponse(
                feedback.id(),
                feedback.type(),
                feedback.title(),
                feedback.status(),
                feedback.createdAt(),
                feedback.hasReplies());
    }
}
