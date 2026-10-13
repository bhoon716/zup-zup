package bhoon.sugang_helper.support.feedback.repository;

import bhoon.sugang_helper.support.feedback.entity.Feedback;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    Page<Feedback> findAllByUserId(Long userId, Pageable pageable);

    List<Feedback> findAllByUserId(Long userId);

    void deleteAllByUserId(Long userId);
}
