package bhoon.sugang_helper.feedback.domain;

import bhoon.sugang_helper.common.domain.RepositoryContract;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FeedbackRepository extends RepositoryContract<Feedback, Long> {
    Page<Feedback> findAllByUserId(Long userId, Pageable pageable);

    List<Feedback> findAllByUserId(Long userId);

    void deleteAllByUserId(Long userId);
}
