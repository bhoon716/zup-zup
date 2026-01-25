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

                Course course = Course.builder()
                                .courseKey("12345-테스트 과목-홍길동")
                                .name("테스트 과목")
                                .professor("홍길동")
                                .capacity(40)
                                .current(30)
                                .build();
                given(courseRepository.searchCourses(any(CourseSearchCondition.class)))
                                .willReturn(List.of(course));

                // when
                List<CourseResponse> responses = courseService.searchCourses(condition);

                // then
                assertThat(responses).hasSize(1);
                assertThat(responses.get(0).getName()).isEqualTo("테스트 과목");
        }

        @Test
        @DisplayName("결과가 없는 경우 빈 리스트 반환")
        void searchCourses_no_result() {
                // given
                CourseSearchCondition condition = CourseSearchCondition.builder().build();
                given(courseRepository.searchCourses(any(CourseSearchCondition.class)))
                                .willReturn(List.of());

                // when
                List<CourseResponse> responses = courseService.searchCourses(condition);

                // then
                assertThat(responses).isEmpty();
        }
}
