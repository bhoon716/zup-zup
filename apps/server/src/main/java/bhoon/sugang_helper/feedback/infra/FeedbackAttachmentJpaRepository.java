package bhoon.sugang_helper.feedback.infra;

import bhoon.sugang_helper.feedback.domain.FeedbackAttachment;
import bhoon.sugang_helper.feedback.domain.FeedbackAttachmentRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackAttachmentJpaRepository extends JpaRepository<FeedbackAttachment, Long>,
        FeedbackAttachmentRepository {

    Optional<FeedbackAttachment> findByIdAndFeedbackId(Long attachmentId, Long feedbackId);
}
