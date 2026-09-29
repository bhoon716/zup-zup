package bhoon.sugang_helper.academic.review.domain;

import bhoon.sugang_helper.common.domain.RepositoryContract;
import java.util.List;
import java.util.Optional;

public interface CourseEmojiReviewRepository extends RepositoryContract<CourseEmojiReview, Long> {
    Optional<CourseEmojiReview> findByCourseKeyAndUserIdAndEmoji(String courseKey, Long userId, String emoji);

    List<CourseEmojiReview> findByCourseKey(String courseKey);

    long countByCourseKeyAndEmoji(String courseKey, String emoji);

    boolean existsByCourseKeyAndUserIdAndEmoji(String courseKey, Long userId, String emoji);

    void deleteAllByUserId(Long userId);

    Optional<CourseEmojiReview> findByCourseKeysAndUserIdAndEmoji(List<String> courseKeys,
                                                                 Long userId,
                                                                 String emoji);

    long countByCourseKeysAndEmoji(List<String> courseKeys, String emoji);

    long countByCourseKeysAndUserIdAndEmoji(List<String> courseKeys, Long userId, String emoji);

    List<Object[]> findEmojiStatsByCourseKey(String courseKey);

    List<Object[]> findEmojiStatsByCourseKeys(List<String> courseKeys);
}
