package bhoon.sugang_helper.support.feedback.dto.response;

import bhoon.sugang_helper.support.feedback.enums.FeedbackStatus;
import bhoon.sugang_helper.support.feedback.enums.FeedbackType;
import java.time.LocalDateTime;
import java.util.List;




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
