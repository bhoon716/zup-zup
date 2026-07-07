package bhoon.sugang_helper.review.domain;

import bhoon.sugang_helper.common.domain.RepositoryContract;
import java.util.Optional;

public interface CourseReviewReactionRepository extends RepositoryContract<CourseReviewReaction, Long> {
    Optional<CourseReviewReaction> findByReviewAndUserId(CourseReview review, Long userId);
}
