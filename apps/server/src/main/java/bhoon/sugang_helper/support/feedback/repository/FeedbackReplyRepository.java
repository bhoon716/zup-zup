package bhoon.sugang_helper.support.feedback.repository;

import bhoon.sugang_helper.support.feedback.entity.FeedbackReply;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackReplyRepository extends JpaRepository<FeedbackReply, Long> {
}
