package bhoon.sugang_helper.feedback.domain;

import bhoon.sugang_helper.common.domain.RepositoryContract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FeedbackRepository extends RepositoryContract<Feedback, Long> {
    Page<Feedback> findAllByUserId(Long userId, Pageable pageable);

    void deleteAllByUserId(Long userId);
}
