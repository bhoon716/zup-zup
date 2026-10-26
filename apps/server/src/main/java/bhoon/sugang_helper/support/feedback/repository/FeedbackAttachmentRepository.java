package bhoon.sugang_helper.support.feedback.repository;

import bhoon.sugang_helper.support.feedback.entity.FeedbackAttachment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackAttachmentRepository extends JpaRepository<FeedbackAttachment, Long> {

    Optional<FeedbackAttachment> findByIdAndFeedbackId(Long attachmentId, Long feedbackId);

    List<FeedbackAttachment> findAllByFeedbackId(Long feedbackId);

    List<FeedbackAttachment> findAllByFeedbackUserId(Long userId);
}
