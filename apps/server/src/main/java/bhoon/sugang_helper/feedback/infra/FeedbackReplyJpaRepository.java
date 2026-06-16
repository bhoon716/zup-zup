package bhoon.sugang_helper.feedback.infra;

import bhoon.sugang_helper.feedback.domain.FeedbackReply;
import bhoon.sugang_helper.feedback.domain.FeedbackReplyRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackReplyJpaRepository extends JpaRepository<FeedbackReply, Long>, FeedbackReplyRepository {
}
