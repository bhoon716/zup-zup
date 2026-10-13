package bhoon.sugang_helper.academic.course.service;

import bhoon.sugang_helper.academic.course.dto.CourseReference;
import java.util.List;
import java.util.Optional;

/**
 * 리뷰 기능이 강의 카탈로그를 조회하고 기존 리뷰 통계를 반영하기 위한 계약입니다.
 */
public interface CourseReviewCatalog {

    Optional<CourseReference> findByCourseKey(String courseKey);

    Optional<CourseReference> findByCourseKeyForUpdate(String courseKey);

    List<String> findCourseKeysBySubjectCodeAndProfessor(String subjectCode, String professor);

    void updateReviewStats(CourseReference course, float averageRating, int reviewCount);
}
