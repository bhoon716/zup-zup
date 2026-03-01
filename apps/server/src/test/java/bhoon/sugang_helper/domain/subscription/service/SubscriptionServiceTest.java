package bhoon.sugang_helper.domain.subscription.service;

import bhoon.sugang_helper.domain.course.service.CourseCrawlerTargetService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import bhoon.sugang_helper.domain.subscription.entity.Subscription;
import bhoon.sugang_helper.domain.subscription.repository.SubscriptionRepository;
import bhoon.sugang_helper.domain.subscription.request.SubscriptionRequest;
import bhoon.sugang_helper.domain.subscription.response.SubscriptionResponse;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CourseCrawlerTargetService crawlerTargetService;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private User user;
    private Course course;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(subscriptionService, "maxLimit", 3);
        user = User.builder().id(1L).email("test@example.com").build();
        course = Course.builder()
                .courseKey("CK1")
                .name("Test Course")
                .professor("Prof")
                .academicYear("2026")
                .semester("U211600010")
                .build();
    }

    @Test
    @DisplayName("구독 신청 성공")
    void subscribe_success() {
        // given
        SubscriptionRequest request = new SubscriptionRequest("CK1");
        try (var mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn(user.getEmail());
            given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));
            given(courseRepository.findByCourseKey(request.getCourseKey())).willReturn(Optional.of(course));
            given(subscriptionRepository.findByUserIdAndCourseKey(user.getId(), course.getCourseKey()))
                    .willReturn(Optional.empty());
            given(subscriptionRepository.countByUserIdAndIsActiveTrue(user.getId())).willReturn(0L);
            given(crawlerTargetService.getCurrentTargetValue())
                    .willReturn(new CourseCrawlerTargetService.CrawlTarget("2026", "U211600010"));

            Subscription subscription = Subscription.builder()
                    .userId(user.getId())
                    .courseKey(course.getCourseKey())
                    .isActive(true)
                    .build();
            ReflectionTestUtils.setField(subscription, "id", 1L);
            given(subscriptionRepository.save(any(Subscription.class))).willReturn(subscription);

            // when
            SubscriptionResponse response = subscriptionService.subscribe(request);

            // then
            assertThat(response.getCourseKey()).isEqualTo(course.getCourseKey());
            assertThat(response.getCourseName()).isEqualTo(course.getName());
        }
    }

    @Test
    @DisplayName("구독 신청 실패 - 중복 구독")
    void subscribe_alreadyExists_throwsException() {
        // given
        SubscriptionRequest request = new SubscriptionRequest("CK1");
        try (var mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn(user.getEmail());
            given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));
            given(courseRepository.findByCourseKey(request.getCourseKey())).willReturn(Optional.of(course));
            given(subscriptionRepository.findByUserIdAndCourseKey(user.getId(), course.getCourseKey()))
                    .willReturn(Optional.of(Subscription.builder().build()));

            // when & then
            assertThatThrownBy(() -> subscriptionService.subscribe(request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode",
                            ErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
        }
    }

    @Test
    @DisplayName("구독 신청 실패 - 최대 개수 초과")
    void subscribe_limitExceeded_throwsException() {
        // given
        SubscriptionRequest request = new SubscriptionRequest("CK1");
        try (var mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn(user.getEmail());
            given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));
            given(courseRepository.findByCourseKey(request.getCourseKey())).willReturn(Optional.of(course));
            given(subscriptionRepository.findByUserIdAndCourseKey(user.getId(), course.getCourseKey()))
                    .willReturn(Optional.empty());
            given(subscriptionRepository.countByUserIdAndIsActiveTrue(user.getId())).willReturn(3L);

            // when & then
            assertThatThrownBy(() -> subscriptionService.subscribe(request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode",
                            ErrorCode.MAX_SUBSCRIPTION_LIMIT_EXCEEDED);
        }
    }
}
