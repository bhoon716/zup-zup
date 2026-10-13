package bhoon.sugang_helper.academic.review.repository;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.academic.course.entity.Course;
import bhoon.sugang_helper.academic.course.repository.CourseRepository;
import bhoon.sugang_helper.academic.review.dto.ReviewScopeKey;
import bhoon.sugang_helper.academic.review.entity.CourseEmojiReview;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class CourseEmojiReviewRepositoryTest {

    private static final String SUBJECT_CODE = "0000131925";
    private static final String LEGACY_COURSE_KEY = "2026:U211600010:0000131925:1";
    private static final String CURRENT_COURSE_KEY = "2026:U211600020:0000131925:1";

    @Autowired
    private CourseEmojiReviewRepository emojiReviewRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    @DisplayName("같은 외부교원N이면 이전 학기 이모지 리뷰를 조회한다")
    void findEmojiStatsBySubjectCodeAndProfessor_loadsLegacyReviewsForExternalInstructor() {
        Course legacyCourse = courseRepository.saveAndFlush(course(LEGACY_COURSE_KEY, "외부교원1", "U211600010"));
        Course currentCourse = courseRepository.saveAndFlush(course(CURRENT_COURSE_KEY, "외부교원1", "U211600020"));
        emojiReviewRepository.saveAndFlush(CourseEmojiReview.builder()
                .courseKey(legacyCourse.getCourseKey())
                .userId(1L)
                .emoji("👍")
                .build());

        ReviewScopeKey scope = ReviewScopeKey.from(currentCourse.getSubjectCode(), currentCourse.getProfessor());
        List<String> courseKeys = courseRepository.findCourseKeysBySubjectCodeAndProfessor(
                scope.subjectCode(), scope.professor());
        List<Object[]> stats = emojiReviewRepository.findEmojiStatsByCourseKeys(courseKeys);

        assertThat(scope.hasIdentifiableProfessor()).isTrue();
        assertThat(stats).singleElement().satisfies(row -> {
            assertThat(row[0]).isEqualTo("👍");
            assertThat(row[1]).isEqualTo(1L);
        });
        assertThat(emojiReviewRepository.countByCourseKeysAndUserIdAndEmoji(courseKeys, 1L, "👍")).isEqualTo(1L);
    }

    @Test
    @DisplayName("실제 교수명이 있는 강의는 같은 과목의 다른 교수 이모지를 섞지 않는다")
    void findEmojiStatsBySubjectCodeAndProfessor_keepsKnownProfessorScoped() {
        Course kimCourse = courseRepository.saveAndFlush(course(LEGACY_COURSE_KEY, "김교수", "U211600010"));
        Course leeCourse = courseRepository.saveAndFlush(course(CURRENT_COURSE_KEY, "이교수", "U211600020"));
        emojiReviewRepository.saveAndFlush(CourseEmojiReview.builder()
                .courseKey(kimCourse.getCourseKey())
                .userId(1L)
                .emoji("👍")
                .build());
        emojiReviewRepository.saveAndFlush(CourseEmojiReview.builder()
                .courseKey(leeCourse.getCourseKey())
                .userId(2L)
                .emoji("🔥")
                .build());

        ReviewScopeKey scope = ReviewScopeKey.from(kimCourse.getSubjectCode(), kimCourse.getProfessor());
        List<String> courseKeys = courseRepository.findCourseKeysBySubjectCodeAndProfessor(
                scope.subjectCode(), scope.professor());
        List<Object[]> stats = emojiReviewRepository.findEmojiStatsByCourseKeys(courseKeys);

        assertThat(stats).singleElement().satisfies(row -> {
            assertThat(row[0]).isEqualTo("👍");
            assertThat(row[1]).isEqualTo(1L);
        });
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
}
