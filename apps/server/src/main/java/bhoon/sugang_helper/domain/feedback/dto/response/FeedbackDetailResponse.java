package bhoon.sugang_helper.domain.feedback.dto.response;

import bhoon.sugang_helper.domain.feedback.entity.Feedback;
import bhoon.sugang_helper.domain.feedback.entity.FeedbackAttachment;
import bhoon.sugang_helper.domain.feedback.entity.enums.FeedbackStatus;
import bhoon.sugang_helper.domain.feedback.entity.enums.FeedbackType;
import java.time.LocalDateTime;
import java.util.List;

public record FeedbackDetailResponse(
        Long id,
        FeedbackType type,
        String title,
        String content,
        FeedbackStatus status,
        String metaInfo,
        LocalDateTime createdAt,
        List<String> imageUrls,
        List<FeedbackReplyResponse> replies) {
    public static FeedbackDetailResponse from(Feedback feedback) {
        return new FeedbackDetailResponse(
                feedback.getId(),
                feedback.getType(),
                feedback.getTitle(),
                feedback.getContent(),
                feedback.getStatus(),
                feedback.getMetaInfo(),
                feedback.getCreatedAt(),
                feedback.getAttachments().stream()
                        .map(FeedbackAttachment::getFileUrl)
                        .toList(),
                feedback.getReplies().stream()
                        .map(FeedbackReplyResponse::from)
                        .toList());
    }
}
