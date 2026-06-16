package bhoon.sugang_helper.feedback.infra;

import bhoon.sugang_helper.feedback.domain.FeedbackAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackAttachmentRepository extends JpaRepository<FeedbackAttachment, Long> {
}
