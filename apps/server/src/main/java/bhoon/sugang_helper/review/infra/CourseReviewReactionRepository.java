package bhoon.sugang_helper.review.infra;

import bhoon.sugang_helper.review.domain.CourseReview;
import bhoon.sugang_helper.review.domain.CourseReviewReaction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseReviewReactionRepository extends JpaRepository<CourseReviewReaction, Long> {

    Optional<CourseReviewReaction> findByReviewAndUserId(CourseReview review, Long userId);
}
