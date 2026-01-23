package bhoon.sugang_helper.domain.subscription.service;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import bhoon.sugang_helper.domain.subscription.request.SubscriptionRequest;
import bhoon.sugang_helper.domain.subscription.response.SubscriptionResponse;
import bhoon.sugang_helper.domain.subscription.entity.Subscription;
import bhoon.sugang_helper.domain.subscription.repository.SubscriptionRepository;
import bhoon.sugang_helper.domain.user.entity.Role;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @InjectMocks
    private SubscriptionService subscriptionService;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private User testUser;
    private Course testCourse;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(subscriptionService, "maxLimit", 3);

        testUser = User.builder()
                .email("test@example.com")
                .name("Tester")
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(testUser, "id", 1L);

        testCourse = Course.builder()
                .courseKey("12345-01")
                .name("Test Course")
                .professor("Test Prof")
                .subjectCode("12345")
                .classNumber("01")
                .capacity(40)
                .current(39)
                .build();

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("구독 신청 성공")
    void subscribe_success() {
        // given
        SubscriptionRequest request = new SubscriptionRequest("12345-01");
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getName()).willReturn(testUser.getEmail());
        given(userRepository.findByEmail(testUser.getEmail())).willReturn(Optional.of(testUser));
        given(courseRepository.findById(request.getCourseKey())).willReturn(Optional.of(testCourse));
        given(subscriptionRepository.findByUserIdAndCourseKey(testUser.getId(), testCourse.getCourseKey()))
                .willReturn(Optional.empty());
        given(subscriptionRepository.countByUserIdAndIsActiveTrue(testUser.getId())).willReturn(0L);

        Subscription subscription = Subscription.builder()
                .userId(testUser.getId())
                .courseKey(testCourse.getCourseKey())
                .isActive(true)
                .build();
        given(subscriptionRepository.save(any(Subscription.class))).willReturn(subscription);

        // when
        SubscriptionResponse response = subscriptionService.subscribe(request);

        // then
        assertThat(response.getCourseKey()).isEqualTo(testCourse.getCourseKey());
        assertThat(response.getCourseName()).isEqualTo(testCourse.getName());
        verify(subscriptionRepository, times(1)).save(any(Subscription.class));
    }

    @Test
    @DisplayName("구독 한도 초과 시 예외 발생")
    void subscribe_limit_exceeded() {
        // given
        SubscriptionRequest request = new SubscriptionRequest("12345-01");
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getName()).willReturn(testUser.getEmail());
        given(userRepository.findByEmail(testUser.getEmail())).willReturn(Optional.of(testUser));
        given(courseRepository.findById(request.getCourseKey())).willReturn(Optional.of(testCourse));
        given(subscriptionRepository.countByUserIdAndIsActiveTrue(testUser.getId())).willReturn(3L);

        // when & then
        assertThatThrownBy(() -> subscriptionService.subscribe(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MAX_SUBSCRIPTION_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("중복 구독 시 예외 발생")
    void subscribe_duplicate() {
        // given
        SubscriptionRequest request = new SubscriptionRequest("12345-01");
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getName()).willReturn(testUser.getEmail());
        given(userRepository.findByEmail(testUser.getEmail())).willReturn(Optional.of(testUser));
        given(courseRepository.findById(request.getCourseKey())).willReturn(Optional.of(testCourse));
        given(subscriptionRepository.findByUserIdAndCourseKey(testUser.getId(), testCourse.getCourseKey()))
                .willReturn(Optional.of(mock(Subscription.class)));

        // when & then
        assertThatThrownBy(() -> subscriptionService.subscribe(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
    }
}
