package bhoon.sugang_helper.academic.review.infra;

import bhoon.sugang_helper.academic.review.domain.CourseEmojiReview;
import bhoon.sugang_helper.academic.review.domain.CourseEmojiReviewRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseEmojiReviewJpaRepository extends JpaRepository<CourseEmojiReview, Long>,
        CourseEmojiReviewRepository {
    Optional<CourseEmojiReview> findByCourseKeyAndUserIdAndEmoji(String courseKey, Long userId, String emoji);

    List<CourseEmojiReview> findByCourseKey(String courseKey);

    long countByCourseKeyAndEmoji(String courseKey, String emoji);

    boolean existsByCourseKeyAndUserIdAndEmoji(String courseKey, Long userId, String emoji);

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

    @Query("select count(r) from CourseEmojiReview r where r.courseKey in :courseKeys and r.emoji = :emoji")
    long countByCourseKeysAndEmoji(@Param("courseKeys") List<String> courseKeys,
                                   @Param("emoji") String emoji);

    @Query("""
            select count(r)
            from CourseEmojiReview r
            where r.courseKey in :courseKeys
              and r.userId = :userId
              and r.emoji = :emoji
            """)
    long countByCourseKeysAndUserIdAndEmoji(@Param("courseKeys") List<String> courseKeys,
                                            @Param("userId") Long userId,
                                            @Param("emoji") String emoji);

    @Query("""
            select r.emoji, count(r)
            from CourseEmojiReview r
            where r.courseKey = :courseKey
            group by r.emoji
            order by count(r) desc, r.emoji asc
            """)
    List<Object[]> findEmojiStatsByCourseKey(@Param("courseKey") String courseKey);

    @Query("""
            select r.emoji, count(r)
            from CourseEmojiReview r
            where r.courseKey in :courseKeys
            group by r.emoji
            order by count(r) desc, r.emoji asc
            """)
    List<Object[]> findEmojiStatsByCourseKeys(@Param("courseKeys") List<String> courseKeys);
}
