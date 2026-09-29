package bhoon.sugang_helper.academic.review.domain;

import bhoon.sugang_helper.common.domain.RepositoryContract;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseReviewRepository extends RepositoryContract<CourseReview, Long> {
    Optional<CourseReview> findByCourseKeyAndUserId(String courseKey, Long userId);

    Optional<CourseReview> findByCourseKeysAndUserId(java.util.List<String> courseKeys, Long userId);

    Page<CourseReview> findByCourseKey(String courseKey, Pageable pageable);

    Page<CourseReview> findByCourseKeysWithMyReviewFirst(java.util.List<String> courseKeys,
                                                         Long userId,
                                                         Pageable pageable);

    Page<CourseReview> findByCourseKeyWithMyReviewFirst(String courseKey,
                                                        Long userId,
                                                        Pageable pageable);

    boolean existsByCourseKeyAndUserId(String courseKey, Long userId);

    void deleteAllByUserId(Long userId);

    long countByCourseKeysAndUserId(java.util.List<String> courseKeys, Long userId);

    long countByCourseKey(String courseKey);

    long countByCourseKeys(java.util.List<String> courseKeys);

    Double getAverageRatingByCourseKey(String courseKey);

    Double getAverageRatingByCourseKeys(java.util.List<String> courseKeys);

    void incrementLikeCount(Long reviewId);

    void decrementLikeCount(Long reviewId);

    void incrementDislikeCount(Long reviewId);

    void decrementDislikeCount(Long reviewId);
}
