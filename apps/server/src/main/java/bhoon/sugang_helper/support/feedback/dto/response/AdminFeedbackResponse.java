package bhoon.sugang_helper.support.feedback.dto.response;

import bhoon.sugang_helper.support.feedback.dto.result.AdminFeedbackSummary;
import bhoon.sugang_helper.support.feedback.enums.FeedbackStatus;
import bhoon.sugang_helper.support.feedback.enums.FeedbackType;
import java.time.LocalDateTime;

public record AdminFeedbackResponse(
        Long id,
        FeedbackType type,
        String title,
        FeedbackStatus status,
        LocalDateTime createdAt,
        boolean hasReplies,
        boolean deleted,
        LocalDateTime deletedAt,
        String authorLabel) {

    public static AdminFeedbackResponse from(AdminFeedbackSummary feedback, String authorLabel) {
        return new AdminFeedbackResponse(
                feedback.id(),
                feedback.type(),
                feedback.title(),
                feedback.status(),
                feedback.createdAt(),
                feedback.hasReplies(),
                feedback.deleted(),
                feedback.deletedAt(),
                authorLabel);
    }
}
