package bhoon.sugang_helper.feedback.infra;

import bhoon.sugang_helper.feedback.domain.FeedbackReply;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackReplyRepository extends JpaRepository<FeedbackReply, Long> {
}
