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

    /**
     * 같은 과목코드와 교수명으로 묶인 리뷰 중, 특정 사용자가 작성한 리뷰를 조회합니다.
     */
    @Query("select r from CourseReview r where r.courseKey in :courseKeys and r.userId = :userId")
    Optional<CourseReview> findByCourseKeysAndUserId(@Param("courseKeys") List<String> courseKeys,
                                                     @Param("userId") Long userId);

    /**
     * 같은 과목코드와 교수명으로 묶인 리뷰 목록을 페이징하여 조회합니다.
     */
    @Query("""
            select r
            from CourseReview r
            where r.courseKey in :courseKeys
            order by case when r.userId = :userId then 0 else 1 end asc, r.createdAt desc
            """)
    Page<CourseReview> findByCourseKeysWithMyReviewFirst(@Param("courseKeys") List<String> courseKeys,
                                                         @Param("userId") Long userId,
                                                         Pageable pageable);

    /**
     * 같은 과목코드와 교수명으로 묶인 리뷰에 대해 특정 사용자가 작성했는지 확인합니다.
     */
    @Query("select count(r) from CourseReview r where r.courseKey in :courseKeys and r.userId = :userId")
    long countByCourseKeysAndUserId(@Param("courseKeys") List<String> courseKeys,
                                    @Param("userId") Long userId);

    /**
     * 같은 과목코드와 교수명으로 묶인 리뷰의 전체 개수를 조회합니다.
     */
    @Query("select count(r) from CourseReview r where r.courseKey in :courseKeys")
    long countByCourseKeys(@Param("courseKeys") List<String> courseKeys);

    /**
     * 같은 과목코드와 교수명으로 묶인 리뷰의 평균 별점을 조회합니다.
     */
    @Query("select coalesce(avg(r.rating), 0.0) from CourseReview r where r.courseKey in :courseKeys")
    Double getAverageRatingByCourseKeys(@Param("courseKeys") List<String> courseKeys);

    void deleteAllByUserId(Long userId);
}
