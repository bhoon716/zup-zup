package bhoon.sugang_helper.academic.review.repository;

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
            where r.courseKey in :courseKeys
            group by r.emoji
            order by count(r) desc, r.emoji asc
            """)
    List<Object[]> findEmojiStatsByCourseKeys(@Param("courseKeys") List<String> courseKeys);

    void deleteAllByUserId(Long userId);
}
