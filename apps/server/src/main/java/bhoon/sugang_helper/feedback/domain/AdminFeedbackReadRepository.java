package bhoon.sugang_helper.feedback.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 전역 SQL restriction에 의존하지 않는 관리자 전용 원시 projection 읽기 모델입니다.
 */
public interface AdminFeedbackReadRepository {

    Page<FeedbackSummary> findFeedbacks(AdminFeedbackDeletionFilter deletionFilter, Pageable pageable);

    Optional<FeedbackDetail> findFeedbackDetail(Long feedbackId);

    List<FeedbackReplySummary> findReplies(Long feedbackId);

    List<FeedbackAttachmentSummary> findAttachments(Long feedbackId);

    Optional<FeedbackAttachmentAccess> findAttachmentForAdmin(Long feedbackId, Long attachmentId);

    record FeedbackSummary(
            Long id,
            FeedbackType type,
            String title,
            FeedbackStatus status,
            LocalDateTime createdAt,
            boolean hasReplies,
            boolean deleted,
            LocalDateTime deletedAt,
            boolean authorWithdrawn) {
    }

    record FeedbackDetail(
            Long id,
            FeedbackType type,
            String title,
            String content,
            FeedbackStatus status,
            LocalDateTime createdAt,
            boolean deleted,
            LocalDateTime deletedAt,
            boolean authorWithdrawn) {
    }

    record FeedbackReplySummary(
            Long id,
            String content,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            boolean deleted,
            LocalDateTime deletedAt) {
    }

    record FeedbackAttachmentSummary(Long id) {
    }

    record FeedbackAttachmentAccess(Long id, Long feedbackId, String fileUrl, String originalName) {
    }
}
