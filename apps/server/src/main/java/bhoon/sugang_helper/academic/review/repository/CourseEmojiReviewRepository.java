package bhoon.sugang_helper.academic.review.repository;

import bhoon.sugang_helper.academic.review.dto.result.CourseEmojiStats;
import bhoon.sugang_helper.academic.review.entity.CourseEmojiReview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseEmojiReviewRepository extends JpaRepository<CourseEmojiReview, Long> {
    @Query("""
            select r
            from CourseEmojiReview r
            where r.courseKey in :courseKeys
              and r.userId = :userId
              and r.emoji = :emoji
            """)
    Optional<CourseEmojiReview> findByCourseKeysAndUserIdAndEmoji(@Param("courseKeys") List<String> courseKeys,
                                                                  @Param("userId") Long userId,
                                                                  @Param("emoji") String emoji);

    @Query("""
            select new bhoon.sugang_helper.academic.review.dto.result.CourseEmojiStats(
                r.emoji, count(r), max(case when r.userId = :userId then 1 else 0 end))
            from CourseEmojiReview r
            where r.courseKey in :courseKeys
            group by r.emoji
            order by count(r) desc, r.emoji asc
            """)
    List<CourseEmojiStats> findEmojiStatsByCourseKeys(@Param("courseKeys") List<String> courseKeys, @Param("userId") Long userId);

    void deleteAllByUserId(Long userId);
}
