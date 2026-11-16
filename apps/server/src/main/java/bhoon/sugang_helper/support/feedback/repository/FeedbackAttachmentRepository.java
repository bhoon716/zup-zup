package bhoon.sugang_helper.support.feedback.repository;

import bhoon.sugang_helper.support.feedback.entity.FeedbackAttachment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedbackAttachmentRepository extends JpaRepository<FeedbackAttachment, Long> {

    @Query("""
            select a from FeedbackAttachment a
            join fetch a.feedback f
            where a.id = :attachmentId and f.id = :feedbackId
            """)
    Optional<FeedbackAttachment> findByIdAndFeedbackId(
            @Param("attachmentId") Long attachmentId, @Param("feedbackId") Long feedbackId);

    List<FeedbackAttachment> findAllByFeedbackId(Long feedbackId);

    List<FeedbackAttachment> findAllByFeedbackUserId(Long userId);
}
