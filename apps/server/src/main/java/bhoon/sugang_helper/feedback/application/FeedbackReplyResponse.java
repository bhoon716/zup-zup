package bhoon.sugang_helper.feedback.application;

import bhoon.sugang_helper.feedback.domain.FeedbackReply;
import java.time.LocalDateTime;

public record FeedbackReplyResponse(
        Long id,
        String adminName,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public static FeedbackReplyResponse from(FeedbackReply reply) {
        return new FeedbackReplyResponse(
                reply.getId(),
                reply.getAdmin().getName(),
                reply.getContent(),
                reply.getCreatedAt(),
                reply.getUpdatedAt());
    }
}
