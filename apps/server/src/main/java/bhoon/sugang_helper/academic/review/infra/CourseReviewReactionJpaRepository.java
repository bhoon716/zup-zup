package bhoon.sugang_helper.academic.review.infra;

import bhoon.sugang_helper.academic.review.domain.CourseReview;
import bhoon.sugang_helper.academic.review.domain.CourseReviewReaction;
import bhoon.sugang_helper.academic.review.domain.CourseReviewReactionRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseReviewReactionJpaRepository extends JpaRepository<CourseReviewReaction, Long>,
        CourseReviewReactionRepository {

    Optional<CourseReviewReaction> findByReviewAndUserId(CourseReview review, Long userId);
}
