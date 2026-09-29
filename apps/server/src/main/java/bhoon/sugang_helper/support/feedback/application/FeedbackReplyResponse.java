package bhoon.sugang_helper.support.feedback.application;

import bhoon.sugang_helper.support.feedback.domain.FeedbackReply;
import java.time.LocalDateTime;

public record FeedbackReplyResponse(
        Long id,
        String adminName,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public static FeedbackReplyResponse from(FeedbackReply reply, String adminName) {
        return new FeedbackReplyResponse(
                reply.getId(),
                adminName,
                reply.getContent(),
                reply.getCreatedAt(),
                reply.getUpdatedAt());
    }
}
