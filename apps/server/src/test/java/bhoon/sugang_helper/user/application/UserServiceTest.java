package bhoon.sugang_helper.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.feedback.domain.FeedbackRepository;
import bhoon.sugang_helper.notification.infra.NotificationChannel;
import bhoon.sugang_helper.notification.application.NotificationService;
import bhoon.sugang_helper.notification.domain.NotificationHistoryRepository;
import bhoon.sugang_helper.review.domain.CourseEmojiReviewRepository;
import bhoon.sugang_helper.review.domain.CourseReviewRepository;
import bhoon.sugang_helper.subscription.domain.SubscriptionRepository;
import bhoon.sugang_helper.timetable.domain.Timetable;
import bhoon.sugang_helper.timetable.domain.TimetableRepository;
import bhoon.sugang_helper.user.application.command.CompleteOnboardingCommand;
import bhoon.sugang_helper.user.application.command.UpdateProfileCommand;
import bhoon.sugang_helper.user.application.command.UpdateSettingsCommand;
import bhoon.sugang_helper.user.application.result.UserOnboardingResult;
import bhoon.sugang_helper.user.application.result.UserProfileResult;
import bhoon.sugang_helper.user.application.result.UserProfileUpdateResult;
import bhoon.sugang_helper.user.application.result.UserSettingsResult;
import bhoon.sugang_helper.user.domain.UserDeviceRepository;
import bhoon.sugang_helper.user.domain.Role;
import bhoon.sugang_helper.user.domain.User;
import bhoon.sugang_helper.user.domain.UserRepository;
import bhoon.sugang_helper.wishlist.domain.WishlistRepository;
import java.util.Optional;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String TEST_EMAIL = "test@example.com";
    private static final String NEW_EMAIL = "new@example.com";
    private static final String TEST_NAME = "Tester";
    private static final String OLD_NAME = "Old Name";
    private static final String NAME = "Name";
    private static final String NEW_NAME = "New Name";
    private static final String TEST_USER = "Test User";
    private static final String NOTIFY_EMAIL = "notify@example.com";

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserDeviceRepository userDeviceRepository;

    @Mock
    private TimetableRepository timetableRepository;

    @Mock
    private CourseReviewRepository courseReviewRepository;

    @Mock
    private CourseEmojiReviewRepository courseEmojiReviewRepository;

    @Mock
    private NotificationHistoryRepository notificationHistoryRepository;

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private UserService userService;

    private MockedStatic<SecurityUtil> securityUtil;

    @BeforeEach
    void setUp() {
        securityUtil = mockStatic(SecurityUtil.class);
    }

    @AfterEach
    void tearDown() {
        securityUtil.close();
    }

    @Test
    @DisplayName("사용자 프로필 정보를 정상적으로 수정한다")
    void updateProfile() {
        // given
        User user = User.builder()
                .id(1L)
                .email(TEST_EMAIL)
                .name(OLD_NAME)
                .role(Role.USER)
                .build();

        securityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));

        // when
        UserProfileUpdateResult result = userService.updateProfile(new UpdateProfileCommand(NEW_NAME));

        // then
        assertThat(result.name()).isEqualTo(NEW_NAME);
        assertThat(user.getName()).isEqualTo(NEW_NAME);
    }

    @Test
    @DisplayName("회원 탈퇴 시 사용자 소유 데이터를 모두 삭제한다")
    void withdraw() {
        // given
        User user = User.builder()
                .id(1L)
                .email(TEST_EMAIL)
                .name(NAME)
                .role(Role.USER)
                .build();
        Timetable primaryTimetable = Timetable.builder()
                .userId(1L)
                .name("대표")
                .isPrimary(true)
                .build();
        Timetable backupTimetable = Timetable.builder()
                .userId(1L)
                .name("보조")
                .isPrimary(false)
                .build();

        securityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
        when(timetableRepository.findByUserId(1L)).thenReturn(List.of(primaryTimetable, backupTimetable));

        // when
        userService.withdraw();

        // then
        verify(subscriptionRepository, times(1)).deleteAllByUserId(1L);
        verify(userDeviceRepository, times(1)).deleteAllByUserId(1L);
        verify(courseReviewRepository, times(1)).deleteAllByUserId(1L);
        verify(courseEmojiReviewRepository, times(1)).deleteAllByUserId(1L);
        verify(notificationHistoryRepository, times(1)).deleteAllByUserId(1L);
        verify(feedbackRepository, times(1)).deleteAllByUserId(1L);
        verify(wishlistRepository, times(1)).deleteAllByUserId(1L);
        verify(timetableRepository, times(1)).delete(primaryTimetable);
        verify(timetableRepository, times(1)).delete(backupTimetable);
        verify(userRepository, times(1)).delete(user);
    }

    @Test
    @DisplayName("내 프로필 정보를 조회한다")
    void getMyProfile() {
        // given
        User user = User.builder()
                .id(1L)
                .email(TEST_EMAIL)
                .name(TEST_USER)
                .role(Role.USER)
                .build();

        securityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));

        // when
        UserProfileResult result = userService.getMyProfile();

        // then
        assertThat(result.email()).isEqualTo(TEST_EMAIL);
        assertThat(result.name()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("사용자 정보를 찾을 수 없으면 예외를 발생시킨다")
    void getMyProfile_UserNotFound_ThrowsException() {
        // given
        securityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getMyProfile())
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_UNAUTHORIZED);
    }

    @Test
    @DisplayName("사용자 설정을 정상적으로 업데이트한다 (디스코드 포함)")
    void updateSettings() {
        // given
        User user = User.builder()
                .id(1L)
                .email(TEST_EMAIL)
                .name(TEST_NAME)
                .role(Role.USER)
                .build();

        UpdateSettingsCommand request = new UpdateSettingsCommand(
                NEW_EMAIL, true, true, false, true);

        securityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
        when(emailVerificationService.isVerified(1L, NEW_EMAIL)).thenReturn(true);

        // when
        UserSettingsResult result = userService.updateSettings(request);

        // then
        assertThat(result.notificationEmail()).isEqualTo(NEW_EMAIL);
        assertThat(result.emailEnabled()).isTrue();
        assertThat(result.discordEnabled()).isTrue();
        assertThat(user.getNotificationEmail()).isEqualTo(NEW_EMAIL);
        assertThat(user.isDiscordEnabled()).isTrue();
    }

    @Test
    @DisplayName("온보딩을 정상적으로 완료한다")
    void completeOnboarding() {
        // given
        User user = User.builder()
                .id(1L)
                .email(TEST_EMAIL)
                .name(TEST_NAME)
                .role(Role.USER)
                .onboardingCompleted(false)
                .build();

        CompleteOnboardingCommand request = new CompleteOnboardingCommand(
                "notify@example.com", true, true, false, false);

        securityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
        when(emailVerificationService.isVerified(1L, NOTIFY_EMAIL)).thenReturn(true);

        // when
        UserOnboardingResult result = userService.completeOnboarding(request);

        // then
        assertThat(result.onboardingCompleted()).isTrue();
        assertThat(user.isOnboardingCompleted()).isTrue();
        assertThat(user.getNotificationEmail()).isEqualTo(NOTIFY_EMAIL);
    }

    @Test
    @DisplayName("사용자 알림 테스트 발송 시 활성화된 채널로 발송한다")
    void sendTestNotification() {
        // given
        User user = User.builder()
                .id(1L)
                .email(TEST_EMAIL)
                .name(TEST_NAME)
                .role(Role.USER)
                .emailEnabled(true)
                .webPushEnabled(true)
                .fcmEnabled(false)
                .discordEnabled(false)
                .build();

        securityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));

        // when
        userService.sendTestNotification();

        // then
        verify(notificationService, times(1)).sendUserTestNotification(eq(user),
                argThat(channels -> channels.size() == 2
                        && channels.contains(NotificationChannel.EMAIL)
                        && channels.contains(NotificationChannel.WEB)));
    }

    @Test
    @DisplayName("활성화된 알림 채널이 없으면 사용자 알림 테스트 발송에 실패한다")
    void sendTestNotification_NoChannel() {
        // given
        User user = User.builder()
                .id(1L)
                .email(TEST_EMAIL)
                .name(TEST_NAME)
                .role(Role.USER)
                .emailEnabled(false)
                .webPushEnabled(false)
                .fcmEnabled(false)
                .discordEnabled(false)
                .build();

        securityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> userService.sendTestNotification())
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }
}
