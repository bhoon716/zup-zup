package bhoon.sugang_helper.review.infra;

import bhoon.sugang_helper.review.domain.CourseEmojiReview;
import bhoon.sugang_helper.review.domain.CourseEmojiReviewRepository;
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
            join Course c on c.courseKey = r.courseKey
            where c.subjectCode = :subjectCode
              and coalesce(trim(c.professor), '') = :professor
              and r.userId = :userId
              and r.emoji = :emoji
            """)
    Optional<CourseEmojiReview> findBySubjectCodeAndProfessorAndUserIdAndEmoji(@Param("subjectCode") String subjectCode,
                                                                               @Param("professor") String professor,
                                                                               @Param("userId") Long userId,
                                                                               @Param("emoji") String emoji);

    @Query("""
            select count(r)
            from CourseEmojiReview r
            join Course c on c.courseKey = r.courseKey
            where c.subjectCode = :subjectCode
              and coalesce(trim(c.professor), '') = :professor
              and r.emoji = :emoji
            """)
    long countBySubjectCodeAndProfessorAndEmoji(@Param("subjectCode") String subjectCode,
                                                @Param("professor") String professor,
                                                @Param("emoji") String emoji);

    @Query("""
            select count(r)
            from CourseEmojiReview r
            join Course c on c.courseKey = r.courseKey
            where c.subjectCode = :subjectCode
              and coalesce(trim(c.professor), '') = :professor
              and r.userId = :userId
              and r.emoji = :emoji
            """)
    long countBySubjectCodeAndProfessorAndUserIdAndEmoji(@Param("subjectCode") String subjectCode,
                                                         @Param("professor") String professor,
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
            join Course c on c.courseKey = r.courseKey
            where c.subjectCode = :subjectCode
              and coalesce(trim(c.professor), '') = :professor
            group by r.emoji
            order by count(r) desc, r.emoji asc
            """)
    List<Object[]> findEmojiStatsBySubjectCodeAndProfessor(@Param("subjectCode") String subjectCode,
                                                           @Param("professor") String professor);
}
