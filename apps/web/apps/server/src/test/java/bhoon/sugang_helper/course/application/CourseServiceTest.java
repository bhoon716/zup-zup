package bhoon.sugang_helper.course.application;

import bhoon.sugang_helper.course.domain.SemesterType;
import bhoon.sugang_helper.crawling.application.CrawlTargetInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.course.domain.Course;
import bhoon.sugang_helper.course.domain.CourseSearchCriteria;
import bhoon.sugang_helper.course.domain.CourseSeatHistory;
import bhoon.sugang_helper.course.domain.CourseRepository;
import bhoon.sugang_helper.course.domain.CourseSeatHistoryRepository;
import bhoon.sugang_helper.course.application.CourseSearchCondition;
import bhoon.sugang_helper.course.application.CourseDetailResponse;
import bhoon.sugang_helper.course.application.CourseResponse;
import bhoon.sugang_helper.course.application.CourseSeatHistoryResponse;
import bhoon.sugang_helper.crawling.application.CourseCrawlerTargetService;
import bhoon.sugang_helper.review.domain.CourseReviewRepository;
import bhoon.sugang_helper.review.domain.ReviewScopeKey;
import bhoon.sugang_helper.user.domain.UserRepository;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.user.domain.User;
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
        private static final String SUBJECT_CODE = "CSE101";
        private static final String PROFESSOR = "김교수";
        private static final String NORMALIZED_PROFESSOR = ReviewScopeKey.normalizeProfessor(PROFESSOR);
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
        private CourseReviewRepository reviewRepository;

        @Mock
        private CourseCrawlerTargetService crawlerTargetService;


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
                                .subjectCode(SUBJECT_CODE)
                                .name(COURSE_NAME)
                                .professor(PROFESSOR)
                                .capacity(50)
                                .current(10)
                                .averageRating(4.0f)
                                .reviewCount(10)
                                .academicYear(ACADEMIC_YEAR)
                                .semester(SEMESTER)
                                .build();
        }

        private void mockCrawlerTarget() {
                given(crawlerTargetService.getCurrentTargetValue())
                                .willReturn(new CrawlTargetInfo(ACADEMIC_YEAR, SemesterType.fromCode(SEMESTER)));
        }

        @Test
        @DisplayName("조건으로 과목 검색 성공")
        void searchCourses_success() {
                // given
                CourseSearchCondition condition = CourseSearchCondition.builder()
                                .name("테스트")
                                .build();
                Course course = createCourse();

                given(courseRepository.searchCourses(any(CourseSearchCriteria.class), any(Pageable.class)))
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
        @DisplayName("강좌 상세 조회 성공 - 로그인 상태")
        void getCourse_withLogin() {
                // given
                Long userId = 1L;
                User user = User.builder().id(userId).email(USER_EMAIL).build();
                Course course = createCourse();
                securityUtilMockedStatic.when(SecurityUtil::getCurrentUserEmailOrNull).thenReturn(USER_EMAIL);
                given(courseRepository.findByCourseKey(COURSE_KEY)).willReturn(Optional.of(course));
                given(userRepository.findByEmail(USER_EMAIL)).willReturn(Optional.of(user));
                given(reviewRepository.countBySubjectCodeAndProfessorAndUserId(SUBJECT_CODE, NORMALIZED_PROFESSOR,
                                userId)).willReturn(1L);
                mockCrawlerTarget();

                // when
                CourseDetailResponse response = courseService.getCourse(COURSE_KEY);

                // then
                assertThat(response).isNotNull();
                assertThat(response.getCourseKey()).isEqualTo(COURSE_KEY);
                assertThat(response.getIsReviewed()).isTrue();
        }
}
