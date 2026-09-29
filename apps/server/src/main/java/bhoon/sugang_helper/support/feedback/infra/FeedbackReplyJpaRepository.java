package bhoon.sugang_helper.support.feedback.infra;

import bhoon.sugang_helper.support.feedback.domain.FeedbackReply;
import bhoon.sugang_helper.support.feedback.domain.FeedbackReplyRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackReplyJpaRepository extends JpaRepository<FeedbackReply, Long>, FeedbackReplyRepository {
}
