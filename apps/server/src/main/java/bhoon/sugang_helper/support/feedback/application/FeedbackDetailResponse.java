package bhoon.sugang_helper.support.feedback.application;

import bhoon.sugang_helper.support.feedback.domain.Feedback;
import bhoon.sugang_helper.support.feedback.domain.FeedbackStatus;
import bhoon.sugang_helper.support.feedback.domain.FeedbackType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    public static FeedbackDetailResponse from(Feedback feedback, Map<Long, String> adminNamesById) {
        return new FeedbackDetailResponse(
                feedback.getId(),
                feedback.getType(),
                feedback.getTitle(),
                feedback.getContent(),
                feedback.getStatus(),
                feedback.getMetaInfo(),
                feedback.getCreatedAt(),
                feedback.getAttachments().stream()
                        .map(attachment -> "/api/v1/feedbacks/%d/attachments/%d".formatted(
                                feedback.getId(), attachment.getId()))
                        .toList(),
                feedback.getReplies().stream()
                        .map(reply -> FeedbackReplyResponse.from(reply, adminNamesById.get(reply.getAdminId())))
                        .toList());
    }
}
