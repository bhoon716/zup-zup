package bhoon.sugang_helper.domain.course.service;

import bhoon.sugang_helper.domain.course.service.CourseCrawlerTargetService.CrawlTarget;

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
import bhoon.sugang_helper.domain.course.response.CourseDetailResponse;
import bhoon.sugang_helper.domain.course.response.CourseResponse;
import bhoon.sugang_helper.domain.course.response.CourseSeatHistoryResponse;
import bhoon.sugang_helper.domain.review.repository.CourseReviewRepository;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mockStatic;
import org.junit.jupiter.api.AfterEach;
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

        private static final String COURSE_KEY = "CK1";
        private static final String COURSE_NAME = "Test Course";
        private static final String ACADEMIC_YEAR = "2026";
        private static final String SEMESTER = "U211600010";
        private static final String USER_EMAIL = "user@test.com";

        @Mock
        private CourseRepository courseRepository;

        @Mock
        private CourseSeatHistoryRepository courseSeatHistoryRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private CourseCrawlerTargetService crawlerTargetService;

        @Mock
        private CourseReviewRepository reviewRepository;

        private CourseService courseService;
        private MockedStatic<SecurityUtil> securityUtilMockedStatic;

        @BeforeEach
        void setUp() {
                securityUtilMockedStatic = mockStatic(SecurityUtil.class);
                courseService = new CourseService(courseRepository, courseSeatHistoryRepository, userRepository,
                                reviewRepository, crawlerTargetService);
        }

        @AfterEach
        void tearDown() {
                if (securityUtilMockedStatic != null) {
                        securityUtilMockedStatic.close();
                }
        }

        private Course createCourse() {
                return Course.builder()
                                .courseKey(COURSE_KEY)
                                .name(COURSE_NAME)
                                .capacity(50)
                                .current(10)
                                .academicYear(ACADEMIC_YEAR)
                                .semester(SEMESTER)
                                .build();
        }

        private void mockCrawlerTarget() {
                given(crawlerTargetService.getCurrentTargetValue())
                                .willReturn(new CrawlTarget(ACADEMIC_YEAR, SEMESTER));
        }

        @Test
        @DisplayName("조건으로 과목 검색 성공")
        void searchCourses_success() {
                // given
                CourseSearchCondition condition = CourseSearchCondition.builder()
                                .name("테스트")
                                .build();
                Course course = createCourse();

                given(courseRepository.searchCourses(any(CourseSearchCondition.class), any(Pageable.class)))
                                .willReturn(new SliceImpl<>(List.of(course)));
                mockCrawlerTarget();

                // when
                Slice<CourseResponse> responses = courseService.searchCourses(condition, PageRequest.of(0, 10));

                // then
                assertThat(responses.getContent()).hasSize(1);
                assertThat(responses.getContent().get(0).getName()).isEqualTo(COURSE_NAME);
        }

        @Test
        @DisplayName("강좌 상세 조회 성공")
        void getCourse_success() {
                // given
                Course course = createCourse();
                given(courseRepository.findByCourseKey(COURSE_KEY)).willReturn(Optional.of(course));
                mockCrawlerTarget();

                // when
                CourseDetailResponse response = courseService.getCourse(COURSE_KEY);

                // then
                assertThat(response.getName()).isEqualTo(COURSE_NAME);
        }

        @Test
        @DisplayName("강좌 상세 조회 실패 - 존재하지 않는 강좌")
        void getCourse_notFound_throwsException() {
                // given
                String invalidCourseKey = "NOT_FOUND";
                given(courseRepository.findByCourseKey(invalidCourseKey)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> courseService.getCourse(invalidCourseKey))
                                .isInstanceOf(CustomException.class)
                                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
        }

        @Test
        @DisplayName("강좌 시트 이력 조회 성공")
        void getCourseHistory_success() {
                // given
                CourseSeatHistory history = CourseSeatHistory.builder()
                                .courseKey(COURSE_KEY)
                                .capacity(50)
                                .current(10)
                                .build();
                given(courseSeatHistoryRepository.findByCourseKeyOrderByCreatedAtDesc(COURSE_KEY))
                                .willReturn(List.of(history));

                // when
                List<CourseSeatHistoryResponse> responses = courseService.getCourseHistory(COURSE_KEY);

                // then
                assertThat(responses).hasSize(1);
                assertThat(responses.get(0).getCapacity()).isEqualTo(50);
        }

        @Test
        @DisplayName("강좌 상세 조회 성공 - 로그인 및 리뷰 작성 상태")
        void getCourse_withLogin_andReviewed() {
                // given
                Long userId = 1L;
                User user = User.builder().id(userId).email(USER_EMAIL).build();
                Course course = createCourse();

                securityUtilMockedStatic.when(SecurityUtil::getCurrentUserEmailOrNull).thenReturn(USER_EMAIL);
                given(courseRepository.findByCourseKey(COURSE_KEY)).willReturn(Optional.of(course));
                mockCrawlerTarget();
                given(userRepository.findByEmail(USER_EMAIL)).willReturn(Optional.of(user));
                given(reviewRepository.existsByCourseKeyAndUserId(COURSE_KEY, userId)).willReturn(true);

                // when
                CourseDetailResponse response = courseService.getCourse(COURSE_KEY);

                // then
                assertThat(response).isNotNull();
                assertThat(response.getIsReviewed()).isTrue();
                assertThat(response.getCourseKey()).isEqualTo(COURSE_KEY);
        }
}
