package bhoon.sugang_helper.domain.feedback.repository;

import bhoon.sugang_helper.domain.feedback.entity.FeedbackAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackAttachmentRepository extends JpaRepository<FeedbackAttachment, Long> {
}
