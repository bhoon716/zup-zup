package bhoon.sugang_helper.feedback.presentation;

import bhoon.sugang_helper.feedback.domain.Feedback;
import bhoon.sugang_helper.feedback.domain.FeedbackStatus;
import bhoon.sugang_helper.feedback.domain.FeedbackType;
import java.time.LocalDateTime;

public record FeedbackResponse(
        Long id,
        FeedbackType type,
        String title,
        FeedbackStatus status,
        LocalDateTime createdAt,
        boolean hasReplies) {
    public static FeedbackResponse from(Feedback feedback) {
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getType(),
                feedback.getTitle(),
                feedback.getStatus(),
                feedback.getCreatedAt(),
                !feedback.getReplies().isEmpty());
    }
}
