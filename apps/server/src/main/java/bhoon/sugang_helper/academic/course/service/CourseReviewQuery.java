package bhoon.sugang_helper.academic.course.service;

public interface CourseReviewQuery {

    boolean hasReviewByUser(String courseKey, String subjectCode, String professor, Long userId);
}
