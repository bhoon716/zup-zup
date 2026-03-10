package bhoon.sugang_helper.domain.feedback.repository;

import bhoon.sugang_helper.domain.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    Page<Feedback> findAllByUserId(Long userId, Pageable pageable);
}
