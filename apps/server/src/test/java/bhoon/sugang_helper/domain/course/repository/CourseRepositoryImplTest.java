package bhoon.sugang_helper.domain.course.repository;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.request.CourseSearchCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class CourseRepositoryImplTest {

    @Autowired
    private CourseRepository courseRepository;

    @Test
    @DisplayName("교수명에 여러 값을 입력하면 OR 조건으로 검색한다")
    void searchCourses_matchesMultipleProfessorsWithOr() {
        // given
        saveCourse("CK1", "알고리즘", "김교수", "컴퓨터공학부");
        saveCourse("CK2", "자료구조", "이교수", "전자공학부");
        saveCourse("CK3", "운영체제", "박교수", "기계공학부");

        CourseSearchCondition condition = baseCondition()
                .professor("김교수, 박교수")
                .sortBy("name")
                .build();

        // when
        var response = courseRepository.searchCourses(condition, PageRequest.of(0, 10));

        // then
        assertThat(response.getContent())
                .extracting(Course::getName)
                .containsExactlyInAnyOrder("알고리즘", "운영체제");
    }

    @Test
    @DisplayName("학과명에 여러 값을 입력하면 OR 조건으로 검색한다")
    void searchCourses_matchesMultipleDepartmentsWithOr() {
        // given
        saveCourse("CK1", "알고리즘", "김교수", "컴퓨터공학부");
        saveCourse("CK2", "자료구조", "이교수", "전자공학부");
        saveCourse("CK3", "운영체제", "박교수", "기계공학부");

        CourseSearchCondition condition = baseCondition()
                .department("전자공학부,\n기계공학부")
                .sortBy("name")
                .build();

        // when
        var response = courseRepository.searchCourses(condition, PageRequest.of(0, 10));

        // then
        assertThat(response.getContent())
                .extracting(Course::getName)
                .containsExactlyInAnyOrder("자료구조", "운영체제");
    }

    private CourseSearchCondition.CourseSearchConditionBuilder baseCondition() {
        return CourseSearchCondition.builder()
                .academicYear("2026")
                .semester("U211600010");
    }

    private void saveCourse(String courseKey, String name, String professor, String department) {
        courseRepository.save(
                Course.builder()
                        .courseKey(courseKey)
                        .subjectCode(courseKey)
                        .name(name)
                        .classNumber("1")
                        .professor(professor)
                        .capacity(50)
                        .current(10)
                        .academicYear("2026")
                        .semester("U211600010")
                        .department(department)
                        .build());
    }
}
