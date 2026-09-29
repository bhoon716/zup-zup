package bhoon.sugang_helper.academic.course.contract;

import java.util.Optional;

/**
 * 리뷰 기능이 강의 카탈로그를 조회하고 기존 리뷰 통계를 반영하기 위한 계약입니다.
 */
public interface CourseReviewCatalog {

    Optional<CourseReference> findByCourseKey(String courseKey);

    Optional<CourseReference> findByCourseKeyForUpdate(String courseKey);

    void updateReviewStats(CourseReference course, float averageRating, int reviewCount);
}
