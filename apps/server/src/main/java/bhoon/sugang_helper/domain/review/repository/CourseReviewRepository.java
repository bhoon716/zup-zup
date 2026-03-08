package bhoon.sugang_helper.domain.review.repository;

import bhoon.sugang_helper.domain.review.entity.CourseReview;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

    /**
     * 특정 사용자가 특정 강의에 작성한 리뷰를 조회합니다.
     */
    Optional<CourseReview> findByCourseKeyAndUserId(String courseKey, Long userId);

    /**
     * 특정 강의의 리뷰 목록을 페이징하여 조회합니다.
     */
    Page<CourseReview> findByCourseKey(String courseKey, Pageable pageable);

    /**
     * 특정 강의의 리뷰 목록을 조회하되, 현재 사용자가 작성한 리뷰를 가장 위에 표시합니다.
     */
    @Query("SELECT r FROM CourseReview r WHERE r.courseKey = :courseKey " +
            "ORDER BY CASE WHEN r.userId = :userId THEN 0 ELSE 1 END ASC, r.createdAt DESC")
    Page<CourseReview> findByCourseKeyWithMyReviewFirst(@Param("courseKey") String courseKey,
            @Param("userId") Long userId, Pageable pageable);

    /**
     * 특정 사용자가 해당 강의에 리뷰를 작성했는지 여부를 확인합니다.
     */
    boolean existsByCourseKeyAndUserId(String courseKey, Long userId);

    /**
     * 특정 강의의 전체 리뷰 개수를 조회합니다.
     */
    long countByCourseKey(String courseKey);

    /**
     * 특정 강의의 평균 별점을 조회합니다. 리뷰가 없으면 0.0을 반환합니다.
     */
    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM CourseReview r WHERE r.courseKey = :courseKey")
    Double getAverageRatingByCourseKey(@Param("courseKey") String courseKey);
}
