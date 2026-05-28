package bhoon.sugang_helper.domain.review.repository;

import bhoon.sugang_helper.domain.review.entity.CourseEmojiReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseEmojiReviewRepository extends JpaRepository<CourseEmojiReview, Long> {
    Optional<CourseEmojiReview> findByCourseKeyAndUserIdAndEmoji(String courseKey, Long userId, String emoji);
    List<CourseEmojiReview> findByCourseKey(String courseKey);
    long countByCourseKeyAndEmoji(String courseKey, String emoji);
    boolean existsByCourseKeyAndUserIdAndEmoji(String courseKey, Long userId, String emoji);

    @Query("""
            select r.emoji, count(r)
            from CourseEmojiReview r
            where r.courseKey = :courseKey
            group by r.emoji
            order by count(r) desc, r.emoji asc
            """)
    List<Object[]> findEmojiStatsByCourseKey(@Param("courseKey") String courseKey);
}
