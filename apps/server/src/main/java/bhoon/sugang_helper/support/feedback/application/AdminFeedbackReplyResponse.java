package bhoon.sugang_helper.support.feedback.application;

import java.time.LocalDateTime;

/**
 * 관리자 전용 답변 응답입니다. 작성자 실명은 노출하지 않습니다.
 */
public record AdminFeedbackReplyResponse(
        Long id,
        String authorLabel,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean deleted,
        LocalDateTime deletedAt) {
}
