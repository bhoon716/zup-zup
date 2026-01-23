package bhoon.sugang_helper.domain.course.service;

import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import bhoon.sugang_helper.domain.course.response.CourseResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @InjectMocks
    private CourseService courseService;

    @Mock
    private CourseRepository courseRepository;

    @Test
    @DisplayName("키워드로 과목 검색 성공")
    void searchCourses_success() {
        // given
        String keyword = "테스트";
        Course course = Course.builder()
                .courseKey("12345-01")
                .name("테스트 과목")
                .professor("홍길동")
                .capacity(40)
                .current(30)
                .build();
        given(courseRepository.findByNameContainingOrProfessorContaining(keyword, keyword))
                .willReturn(List.of(course));

        // when
        List<CourseResponse> responses = courseService.searchCourses(keyword);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("테스트 과목");
        assertThat(responses.get(0).getProfessor()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("빈 키워드 검색 시 빈 리스트 반환")
    void searchCourses_empty_keyword() {
        // when
        List<CourseResponse> responses = courseService.searchCourses(" ");

        // then
        assertThat(responses).isEmpty();
    }
}
