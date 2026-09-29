package bhoon.sugang_helper.support.feedback.infra;

import bhoon.sugang_helper.support.feedback.domain.FeedbackAttachment;
import bhoon.sugang_helper.support.feedback.domain.FeedbackAttachmentRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackAttachmentJpaRepository extends JpaRepository<FeedbackAttachment, Long>,
        FeedbackAttachmentRepository {

    Optional<FeedbackAttachment> findByIdAndFeedbackId(Long attachmentId, Long feedbackId);
}
