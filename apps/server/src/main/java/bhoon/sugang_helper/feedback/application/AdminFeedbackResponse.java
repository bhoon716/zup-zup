package bhoon.sugang_helper.feedback.application;

import bhoon.sugang_helper.feedback.domain.FeedbackStatus;
import bhoon.sugang_helper.feedback.domain.FeedbackType;
import java.time.LocalDateTime;

/**
 * 관리자 전용 피드백 목록 응답입니다. 작성자 식별자는 노출하지 않습니다.
 */
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
}
