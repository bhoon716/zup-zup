package bhoon.sugang_helper.domain.review.repository;

import bhoon.sugang_helper.domain.review.entity.CourseEmojiReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseEmojiReviewRepository extends JpaRepository<CourseEmojiReview, Long> {
    Optional<CourseEmojiReview> findByCourseKeyAndUserIdAndEmoji(String courseKey, Long userId, String emoji);
    List<CourseEmojiReview> findByCourseKey(String courseKey);
    long countByCourseKeyAndEmoji(String courseKey, String emoji);
    boolean existsByCourseKeyAndUserIdAndEmoji(String courseKey, Long userId, String emoji);
}
