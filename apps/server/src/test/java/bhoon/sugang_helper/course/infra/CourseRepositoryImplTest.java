package bhoon.sugang_helper.course.infra;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.course.domain.Course;
import bhoon.sugang_helper.course.domain.CourseSearchCriteria;
import bhoon.sugang_helper.course.domain.CourseRepository;
import bhoon.sugang_helper.wishlist.domain.Wishlist;
import bhoon.sugang_helper.wishlist.domain.WishlistRepository;
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

        CourseSearchCriteria condition = baseCondition()
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

        CourseSearchCriteria condition = baseCondition()
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

        CourseSearchCriteria condition = baseCondition()
                .sortBy("popular")
                .build();

        // when
        var response = courseRepository.searchCourses(condition, PageRequest.of(0, 10));

        // then
        assertThat(response.getContent())
                .extracting(Course::getName)
                .containsExactly(DS, ALG, OS);
    }

    @Test
    @DisplayName("평점 정렬은 평점 내림차순, 리뷰 개수 내림차순, 과목 키 오름차순으로 정렬한다")
    void searchCourses_sortsByRating() {
        // given
        courseRepository.save(Course.builder()
                .courseKey("CK1").subjectCode("CK1").name(ALG).classNumber("1")
                .academicYear(ACADEMIC_YEAR).semester(SEMESTER_CODE)
                .capacity(50).current(10).averageRating(4.5f).reviewCount(20).build());
        courseRepository.save(Course.builder()
                .courseKey("CK2").subjectCode("CK2").name(DS).classNumber("1")
                .academicYear(ACADEMIC_YEAR).semester(SEMESTER_CODE)
                .capacity(50).current(10).averageRating(3.0f).reviewCount(50).build());
        courseRepository.save(Course.builder()
                .courseKey("CK3").subjectCode("CK3").name(OS).classNumber("1")
                .academicYear(ACADEMIC_YEAR).semester(SEMESTER_CODE)
                .capacity(50).current(10).averageRating(5.0f).reviewCount(10).build());

        CourseSearchCriteria condition = baseCondition()
                .sortBy("rating")
                .sortOrder("desc")
                .build();

        // when
        var response = courseRepository.searchCourses(condition, PageRequest.of(0, 10));

        // then
        assertThat(response.getContent())
                .extracting(Course::getName)
                .containsExactly(OS, ALG, DS); // CK3(5.0, 10), CK1(4.5, 20), CK2(3.0, 50)
    }

    @Test
    @DisplayName("여석 정렬은 여석(정원-신청인원) 내림차순, 과목명 오름차순, 과목 키 오름차순으로 정렬한다")
    void searchCourses_sortsByAvailableSeats() {
        // given
        courseRepository.save(Course.builder()
                .courseKey("CK1").subjectCode("CK1").name(ALG).classNumber("1")
                .academicYear(ACADEMIC_YEAR).semester(SEMESTER_CODE)
                .capacity(50).current(10) // 여석: 40
                .build());
        courseRepository.save(Course.builder()
                .courseKey("CK2").subjectCode("CK2").name(DS).classNumber("1")
                .academicYear(ACADEMIC_YEAR).semester(SEMESTER_CODE)
                .capacity(50).current(40) // 여석: 10
                .build());
        courseRepository.save(Course.builder()
                .courseKey("CK3").subjectCode("CK3").name(OS).classNumber("1")
                .academicYear(ACADEMIC_YEAR).semester(SEMESTER_CODE)
                .capacity(50).current(20) // 여석: 30
                .build());
        courseRepository.save(Course.builder()
                .courseKey("CK4").subjectCode("CK4").name("네트워크").classNumber("1")
                .academicYear(ACADEMIC_YEAR).semester(SEMESTER_CODE)
                .capacity(50).current(20) // 여석: 30
                .build());

        CourseSearchCriteria condition = baseCondition()
                .sortBy("available")
                .sortOrder("desc")
                .build();

        // when
        var response = courseRepository.searchCourses(condition, PageRequest.of(0, 10));

        // then
        assertThat(response.getContent())
                .extracting(Course::getName)
                .containsExactly(ALG, "네트워크", OS, DS); // CK1(40), CK4(30, 네트워크), CK3(30, 운영체제), CK2(10)
    }

    private CourseSearchCriteria.CourseSearchCriteriaBuilder baseCondition() {
        return CourseSearchCriteria.builder()
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
