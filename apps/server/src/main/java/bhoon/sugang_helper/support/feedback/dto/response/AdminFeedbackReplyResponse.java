package bhoon.sugang_helper.support.feedback.dto.response;

import java.time.LocalDateTime;




public record AdminFeedbackReplyResponse(
        Long id,
        String authorLabel,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean deleted,
        LocalDateTime deletedAt) {
}
