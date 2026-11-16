package bhoon.sugang_helper.academic.review.repository;

import bhoon.sugang_helper.academic.review.dto.result.CourseReviewStats;
import bhoon.sugang_helper.academic.review.entity.CourseReview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

    @Query("select r from CourseReview r where r.courseKey in :courseKeys and r.userId = :userId")
    Optional<CourseReview> findByCourseKeysAndUserId(@Param("courseKeys") List<String> courseKeys,
                                                     @Param("userId") Long userId);

    @Query("""
            select r
            from CourseReview r
            where r.courseKey in :courseKeys
            order by case when r.userId = :userId then 0 else 1 end asc, r.createdAt desc
            """)
    Page<CourseReview> findByCourseKeysWithMyReviewFirst(@Param("courseKeys") List<String> courseKeys,
                                                         @Param("userId") Long userId,
                                                         Pageable pageable);

    @Query("select count(r) from CourseReview r where r.courseKey in :courseKeys and r.userId = :userId")
    long countByCourseKeysAndUserId(@Param("courseKeys") List<String> courseKeys,
                                    @Param("userId") Long userId);

    @Query("""
            select new bhoon.sugang_helper.academic.review.dto.result.CourseReviewStats(
                coalesce(avg(r.rating), 0.0), count(r))
            from CourseReview r where r.courseKey in :courseKeys
            """)
    CourseReviewStats findStatsByCourseKeys(@Param("courseKeys") List<String> courseKeys);

    void deleteAllByUserId(Long userId);
}
