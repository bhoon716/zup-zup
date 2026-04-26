package bhoon.sugang_helper.domain.course.repository;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.request.CourseSearchCondition;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

@DataJpaTest
class CourseRepositoryTest {

    @Autowired
    private CourseRepository courseRepository;

    @Test
    @DisplayName("단과대 ID로 강의 검색 성공")
    void searchByCollegeId() {
        // given
        Course course1 = Course.builder()
                .courseKey("KEY1")
                .name("공대강의")
                .subjectCode("S1")
                .classNumber("01")
                .academicYear("2026")
                .semester("S1")
                .collegeId(1L)
                .build();
        Course course2 = Course.builder()
                .courseKey("KEY2")
                .name("자연대강의")
                .subjectCode("S2")
                .classNumber("01")
                .academicYear("2026")
                .semester("S1")
                .collegeId(2L)
                .build();
        courseRepository.saveAll(List.of(course1, course2));

        CourseSearchCondition condition = CourseSearchCondition.builder()
                .collegeId(1L)
                .build();

        // when
        Slice<Course> result = courseRepository.searchCourses(condition, PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("공대강의");
    }

    @Test
    @DisplayName("학과 ID로 강의 검색 성공")
    void searchByDepartmentId() {
        // given
        Course course1 = Course.builder()
                .courseKey("KEY1")
                .name("소학강의")
                .subjectCode("S1")
                .classNumber("01")
                .academicYear("2026")
                .semester("S1")
                .departmentId(101L)
                .build();
        Course course2 = Course.builder()
                .courseKey("KEY2")
                .name("컴공강의")
                .subjectCode("S2")
                .classNumber("01")
                .academicYear("2026")
                .semester("S1")
                .departmentId(102L)
                .build();
        courseRepository.saveAll(List.of(course1, course2));

        CourseSearchCondition condition = CourseSearchCondition.builder()
                .departmentId(101L)
                .build();

        // when
        Slice<Course> result = courseRepository.searchCourses(condition, PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("소학강의");
    }
}
