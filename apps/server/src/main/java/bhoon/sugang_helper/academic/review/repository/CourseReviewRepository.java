package bhoon.sugang_helper.academic.review.repository;

import bhoon.sugang_helper.academic.review.entity.CourseReview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

    @Modifying
    @Query("update CourseReview r set r.likeCount = r.likeCount + 1 where r.id = :reviewId")
    void incrementLikeCount(@Param("reviewId") Long reviewId);

    @Modifying
    @Query("update CourseReview r set r.likeCount = case when r.likeCount > 0 then r.likeCount - 1 else 0 end where r.id = :reviewId")
    void decrementLikeCount(@Param("reviewId") Long reviewId);

    @Modifying
    @Query("update CourseReview r set r.dislikeCount = r.dislikeCount + 1 where r.id = :reviewId")
    void incrementDislikeCount(@Param("reviewId") Long reviewId);

    @Modifying
    @Query("update CourseReview r set r.dislikeCount = case when r.dislikeCount > 0 then r.dislikeCount - 1 else 0 end where r.id = :reviewId")
    void decrementDislikeCount(@Param("reviewId") Long reviewId);




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




    @Query("select count(r) from CourseReview r where r.courseKey in :courseKeys")
    long countByCourseKeys(@Param("courseKeys") List<String> courseKeys);




    @Query("select coalesce(avg(r.rating), 0.0) from CourseReview r where r.courseKey in :courseKeys")
    Double getAverageRatingByCourseKeys(@Param("courseKeys") List<String> courseKeys);

    void deleteAllByUserId(Long userId);
}
