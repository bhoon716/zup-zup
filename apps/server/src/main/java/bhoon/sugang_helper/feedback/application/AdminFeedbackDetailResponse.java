package bhoon.sugang_helper.feedback.application;

import bhoon.sugang_helper.feedback.domain.FeedbackStatus;
import bhoon.sugang_helper.feedback.domain.FeedbackType;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 전용 피드백 상세 응답입니다. 작성자 식별자와 환경 메타데이터는 포함하지 않습니다.
 */
public record AdminFeedbackDetailResponse(
        Long id,
        FeedbackType type,
        String title,
        String content,
        FeedbackStatus status,
        LocalDateTime createdAt,
        boolean deleted,
        LocalDateTime deletedAt,
        String authorLabel,
        List<AdminFeedbackAttachmentResponse> attachments,
        List<AdminFeedbackReplyResponse> replies) {
}
