package bhoon.sugang_helper.academic.review.repository;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.academic.course.entity.Course;
import bhoon.sugang_helper.academic.course.repository.CourseRepository;
import bhoon.sugang_helper.academic.review.entity.CourseReview;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class CourseReviewRepositoryTest {

    private static final String SUBJECT_CODE = "0000131925";
    private static final String PREVIOUS_COURSE_KEY = "2026:U211600010:0000131925:1";
    private static final String CURRENT_COURSE_KEY = "2026:U211600020:0000131925:1";

    @Autowired
    private CourseReviewRepository reviewRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    @DisplayName("같은 외부교원N의 학기별 강의 키를 사용해 리뷰를 묶고 다른 교수를 제외한다")
    void findsReviewsForCourseKeysWithinProfessorScope() {
        Course previousCourse = courseRepository.saveAndFlush(course(PREVIOUS_COURSE_KEY, "외부교원1", "U211600010"));
        Course currentCourse = courseRepository.saveAndFlush(course(CURRENT_COURSE_KEY, "외부교원1", "U211600020"));
        Course differentProfessorCourse = courseRepository.saveAndFlush(
                course("2026:U211600030:0000131925:1", "김교수", "U211600030"));

        CourseReview myReview = reviewRepository.saveAndFlush(review(previousCourse.getCourseKey(), 1L, 5));
        reviewRepository.saveAndFlush(review(currentCourse.getCourseKey(), 2L, 3));
        reviewRepository.saveAndFlush(review(differentProfessorCourse.getCourseKey(), 3L, 1));

        List<String> courseKeys = courseRepository.findCourseKeysBySubjectCodeAndProfessor(SUBJECT_CODE, "외부교원1");
        var page = reviewRepository.findByCourseKeysWithMyReviewFirst(courseKeys, 1L, PageRequest.of(0, 10));

        assertThat(courseKeys).containsExactlyInAnyOrder(PREVIOUS_COURSE_KEY, CURRENT_COURSE_KEY);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().getFirst().getId()).isEqualTo(myReview.getId());
        assertThat(reviewRepository.findByCourseKeysAndUserId(courseKeys, 1L))
                .contains(myReview);
        assertThat(reviewRepository.countByCourseKeys(courseKeys)).isEqualTo(2L);
        assertThat(reviewRepository.countByCourseKeysAndUserId(courseKeys, 1L)).isEqualTo(1L);
        assertThat(reviewRepository.getAverageRatingByCourseKeys(courseKeys)).isEqualTo(4.0);
    }

    private Course course(String courseKey, String professor, String semester) {
        return Course.builder()
                .courseKey(courseKey)
                .subjectCode(SUBJECT_CODE)
                .name("자료구조")
                .classNumber("1")
                .professor(professor)
                .capacity(50)
                .current(10)
                .academicYear("2026")
                .semester(semester)
                .build();
    }

    private CourseReview review(String courseKey, Long userId, int rating) {
        return CourseReview.builder()
                .courseKey(courseKey)
                .userId(userId)
                .rating(rating)
                .build();
    }
}
