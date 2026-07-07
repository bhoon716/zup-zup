package bhoon.sugang_helper.feedback.infra;

import bhoon.sugang_helper.feedback.domain.Feedback;
import bhoon.sugang_helper.feedback.domain.FeedbackRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackJpaRepository extends JpaRepository<Feedback, Long>, FeedbackRepository {
    Page<Feedback> findAllByUserId(Long userId, Pageable pageable);
}
