package bhoon.sugang_helper.domain.feedback.repository;

import bhoon.sugang_helper.domain.feedback.entity.FeedbackReply;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackReplyRepository extends JpaRepository<FeedbackReply, Long> {
}
