package bhoon.sugang_helper.feedback.infra;

import bhoon.sugang_helper.feedback.domain.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    Page<Feedback> findAllByUserId(Long userId, Pageable pageable);
}
