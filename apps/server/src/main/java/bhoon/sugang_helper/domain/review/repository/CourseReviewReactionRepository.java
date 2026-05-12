package bhoon.sugang_helper.domain.review.repository;

import bhoon.sugang_helper.domain.review.entity.CourseReview;
import bhoon.sugang_helper.domain.review.entity.CourseReviewReaction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseReviewReactionRepository extends JpaRepository<CourseReviewReaction, Long> {

    Optional<CourseReviewReaction> findByReviewAndUserId(CourseReview review, Long userId);
}
