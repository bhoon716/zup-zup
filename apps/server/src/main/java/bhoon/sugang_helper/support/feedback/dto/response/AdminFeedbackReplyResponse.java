package bhoon.sugang_helper.support.feedback.dto.response;

import bhoon.sugang_helper.support.feedback.dto.result.AdminFeedbackReplySummary;
import java.time.LocalDateTime;

public record AdminFeedbackReplyResponse(
        Long id,
        String authorLabel,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean deleted,
        LocalDateTime deletedAt) {

    public static AdminFeedbackReplyResponse from(AdminFeedbackReplySummary reply, String authorLabel) {
        return new AdminFeedbackReplyResponse(
                reply.id(),
                authorLabel,
                reply.content(),
                reply.createdAt(),
                reply.updatedAt(),
                reply.deleted(),
                reply.deletedAt());
    }
}
