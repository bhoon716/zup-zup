package bhoon.sugang_helper.domain.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.entity.CourseSeatHistory;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import bhoon.sugang_helper.domain.course.repository.CourseSeatHistoryRepository;
import bhoon.sugang_helper.domain.course.request.CourseSearchCondition;
import bhoon.sugang_helper.domain.course.response.CourseResponse;
import bhoon.sugang_helper.domain.course.response.CourseSeatHistoryResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseSeatHistoryRepository courseSeatHistoryRepository;

    @Mock
    private bhoon.sugang_helper.domain.user.repository.UserRepository userRepository;

    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(courseRepository, courseSeatHistoryRepository, userRepository);
    }

    @Test
    @DisplayName("조건으로 과목 검색 성공")
    void searchCourses_success() {
        // given
        CourseSearchCondition condition = CourseSearchCondition.builder()
                .name("테스트")
                .build();
        Course course = Course.builder()
                .courseKey("CK1")
                .name("Test Course")
                .capacity(50)
                .current(10)
                .build();
        given(courseRepository.searchCourses(any(CourseSearchCondition.class), any(Pageable.class)))
                .willReturn(new SliceImpl<>(List.of(course)));

        // when
        Slice<CourseResponse> responses = courseService.searchCourses(condition, PageRequest.of(0, 10));

        // then
        assertThat(responses.getContent()).hasSize(1);
        assertThat(responses.getContent().get(0).getName()).isEqualTo("Test Course");
    }

    @Test
    @DisplayName("강좌 상세 조회 성공")
    void getCourse_success() {
        // given
        String courseKey = "CK1";
        Course course = Course.builder()
                .courseKey(courseKey)
                .name("Test Course")
                .capacity(50)
                .current(10)
                .build();
        given(courseRepository.findByCourseKey(courseKey)).willReturn(Optional.of(course));

        // when
        CourseResponse response = courseService.getCourse(courseKey);

        // then
        assertThat(response.getName()).isEqualTo("Test Course");
    }

    @Test
    @DisplayName("강좌 상세 조회 실패 - 존재하지 않는 강좌")
    void getCourse_notFound_throwsException() {
        // given
        String courseKey = "NOT_FOUND";
        given(courseRepository.findByCourseKey(courseKey)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> courseService.getCourse(courseKey))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("강좌 시트 이력 조회 성공")
    void getCourseHistory_success() {
        // given
        String courseKey = "CK1";
        CourseSeatHistory history = CourseSeatHistory.builder()
                .courseKey(courseKey)
                .capacity(50)
                .current(10)
                .build();
        given(courseSeatHistoryRepository.findByCourseKeyOrderByCreatedAtDesc(courseKey))
                .willReturn(List.of(history));

        // when
        List<CourseSeatHistoryResponse> responses = courseService.getCourseHistory(courseKey);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getCapacity()).isEqualTo(50);
    }
}
