package bhoon.sugang_helper.support.feedback.dto.result;

import java.time.LocalDateTime;

public record AdminFeedbackReplySummary(
        Long id,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean deleted,
        LocalDateTime deletedAt) {
}
