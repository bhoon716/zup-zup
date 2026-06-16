package bhoon.sugang_helper.course.infra;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.course.domain.Course;
import bhoon.sugang_helper.course.presentation.CourseSearchCondition;
import bhoon.sugang_helper.wishlist.domain.Wishlist;
import bhoon.sugang_helper.wishlist.infra.WishlistRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class CourseRepositoryImplTest {

    private static final String ACADEMIC_YEAR = "2026";
    private static final String SEMESTER_CODE = "U211600010";
    private static final String ALG = "알고리즘";
    private static final String DS = "자료구조";
    private static final String OS = "운영체제";
    private static final String CS = "컴퓨터공학부";
    private static final String EE = "전자공학부";
    private static final String ME = "기계공학부";

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private WishlistRepository wishlistRepository;

    @Test
    @DisplayName("교수명에 여러 값을 입력하면 OR 조건으로 검색한다")
    void searchCourses_matchesMultipleProfessorsWithOr() {
        // given
        saveCourse("CK1", ALG, "김교수", CS);
        saveCourse("CK2", DS, "이교수", EE);
        saveCourse("CK3", OS, "박교수", ME);

        CourseSearchCondition condition = baseCondition()
                .professor("김교수, 박교수")
                .sortBy("name")
                .build();

        // when
        var response = courseRepository.searchCourses(condition, PageRequest.of(0, 10));

        // then
        assertThat(response.getContent())
                .extracting(Course::getName)
                .containsExactlyInAnyOrder(ALG, OS);
    }

    @Test
    @DisplayName("학과명에 여러 값을 입력하면 OR 조건으로 검색한다")
    void searchCourses_matchesMultipleDepartmentsWithOr() {
        // given
        saveCourse("CK1", ALG, "김교수", CS);
        saveCourse("CK2", DS, "이교수", EE);
        saveCourse("CK3", OS, "박교수", ME);

        CourseSearchCondition condition = baseCondition()
                .department("전자공학부,\n기계공학부")
                .sortBy("name")
                .build();

        // when
        var response = courseRepository.searchCourses(condition, PageRequest.of(0, 10));

        // then
        assertThat(response.getContent())
                .extracting(Course::getName)
                .containsExactlyInAnyOrder(DS, OS);
    }

    @Test
    @DisplayName("인기순 정렬은 찜 수 내림차순으로 정렬하고 찜이 없는 강의도 포함한다")
    void searchCourses_sortsByPopularityWithoutFilteringUnwishedCourses() {
        // given
        saveCourse("CK1", ALG, "김교수", CS);
        saveCourse("CK2", DS, "이교수", CS);
        saveCourse("CK3", OS, "박교수", CS);
        saveWishlist(1L, "CK1");
        saveWishlist(2L, "CK2");
        saveWishlist(3L, "CK2");
        saveWishlist(4L, "CK2");

        CourseSearchCondition condition = baseCondition()
                .sortBy("popular")
                .build();

        // when
        var response = courseRepository.searchCourses(condition, PageRequest.of(0, 10));

        // then
        assertThat(response.getContent())
                .extracting(Course::getName)
                .containsExactly(DS, ALG, OS);
    }

    private CourseSearchCondition.CourseSearchConditionBuilder baseCondition() {
        return CourseSearchCondition.builder()
                .academicYear(ACADEMIC_YEAR)
                .semester(SEMESTER_CODE);
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
                        .academicYear(ACADEMIC_YEAR)
                        .semester(SEMESTER_CODE)
                        .department(department)
                        .build());
    }

    private void saveWishlist(Long userId, String courseKey) {
        wishlistRepository.save(Wishlist.builder()
                .userId(userId)
                .courseKey(courseKey)
                .build());
    }
}
