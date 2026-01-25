package bhoon.sugang_helper.domain.course.service;

import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import bhoon.sugang_helper.domain.course.response.CourseResponse;
import bhoon.sugang_helper.domain.course.request.CourseSearchCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @InjectMocks
    private CourseService courseService;

    @Mock
    private CourseRepository courseRepository;

    @Test
    @DisplayName("조건으로 과목 검색 성공")
    void searchCourses_success() {
        // given
        CourseSearchCondition condition = CourseSearchCondition.builder()
                .name("테스트")
                .build();
        Pageable pageable = PageRequest.of(0, 10);

        Course course = Course.builder()
                .courseKey("12345-테스트 과목-홍길동")
                .name("테스트 과목")
                .professor("홍길동")
                .capacity(40)
                .current(30)
                .build();
        given(courseRepository.searchCourses(any(CourseSearchCondition.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(course)));

        // when
        Page<CourseResponse> responses = courseService.searchCourses(condition, pageable);

        // then
        assertThat(responses.getContent()).hasSize(1);
        assertThat(responses.getContent().get(0).getName()).isEqualTo("테스트 과목");
    }

    @Test
    @DisplayName("결과가 없는 경우 빈 페이지 반환")
    void searchCourses_no_result() {
        // given
        CourseSearchCondition condition = CourseSearchCondition.builder().build();
        Pageable pageable = PageRequest.of(0, 10);
        given(courseRepository.searchCourses(any(CourseSearchCondition.class), any(Pageable.class)))
                .willReturn(Page.empty());

        // when
        Page<CourseResponse> responses = courseService.searchCourses(condition, pageable);

        // then
        assertThat(responses.getContent()).isEmpty();
    }
}
