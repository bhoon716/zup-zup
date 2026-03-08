package bhoon.sugang_helper.domain.review.repository;

import bhoon.sugang_helper.domain.review.entity.CourseReview;
import bhoon.sugang_helper.domain.review.entity.CourseReviewReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseReviewReactionRepository extends JpaRepository<CourseReviewReaction, Long> {

    Optional<CourseReviewReaction> findByReviewAndUserId(CourseReview review, Long userId);
}
