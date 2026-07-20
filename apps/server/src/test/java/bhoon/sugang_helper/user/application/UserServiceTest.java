package bhoon.sugang_helper.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.LocalFileUploadService;
import bhoon.sugang_helper.common.security.jwt.JwtProvider;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.feedback.domain.Feedback;
import bhoon.sugang_helper.feedback.domain.FeedbackAttachmentRepository;
import bhoon.sugang_helper.feedback.domain.FeedbackRepository;
import bhoon.sugang_helper.notification.application.NotificationService;
import bhoon.sugang_helper.notification.domain.NotificationHistoryRepository;
import bhoon.sugang_helper.notification.infra.NotificationChannel;
import bhoon.sugang_helper.review.domain.CourseEmojiReviewRepository;
import bhoon.sugang_helper.review.domain.CourseReviewRepository;
import bhoon.sugang_helper.subscription.domain.Subscription;
import bhoon.sugang_helper.subscription.domain.SubscriptionRepository;
import bhoon.sugang_helper.timetable.domain.TimetableRepository;
import bhoon.sugang_helper.user.application.command.CompleteOnboardingCommand;
import bhoon.sugang_helper.user.application.command.UpdateProfileCommand;
import bhoon.sugang_helper.user.application.command.UpdateSettingsCommand;
import bhoon.sugang_helper.user.application.result.UserOnboardingResult;
import bhoon.sugang_helper.user.application.result.UserProfileResult;
import bhoon.sugang_helper.user.application.result.UserProfileUpdateResult;
import bhoon.sugang_helper.user.application.result.UserSettingsResult;
import bhoon.sugang_helper.user.domain.Role;
import bhoon.sugang_helper.user.domain.User;
import bhoon.sugang_helper.user.domain.UserDeviceRepository;
import bhoon.sugang_helper.user.domain.UserRepository;
import bhoon.sugang_helper.wishlist.domain.WishlistRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

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
    private FeedbackAttachmentRepository feedbackAttachmentRepository;

    @Mock
    private LocalFileUploadService fileUploadService;

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private JwtProvider jwtProvider;

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
    @DisplayName("회원 탈퇴 시 식별자는 익명화하고 비식별 이력은 보존한다")
    void withdraw_softDeletesAndAnonymizesAccount() {
        // given
        User user = User.builder()
                .id(1L)
                .email(TEST_EMAIL)
                .name(NAME)
                .notificationEmail(NOTIFY_EMAIL)
                .emailEnabled(true)
                .webPushEnabled(true)
                .fcmEnabled(true)
                .discordEnabled(true)
                .discordId("discord-id")
                .role(Role.USER)
                .build();
        Subscription subscription = Subscription.builder()
                .userId(1L)
                .courseKey("CS101")
                .isActive(true)
                .build();
        Feedback feedback = Feedback.builder()
                .user(user)
                .type(bhoon.sugang_helper.feedback.domain.FeedbackType.BUG)
                .title("제목")
                .content("내용")
                .build();

        securityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserId(1L)).thenReturn(List.of(subscription));
        when(feedbackRepository.findAllByUserId(1L)).thenReturn(List.of(feedback));

        // when
        userService.withdraw();

        // then
        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getEmail()).isEqualTo("deleted-1@deleted.invalid");
        assertThat(user.getName()).isEqualTo("탈퇴한 사용자");
        assertThat(user.getNotificationEmail()).isNull();
        assertThat(user.getDiscordId()).isNull();
        assertThat(user.getEnabledNotificationChannels()).isEmpty();
        assertThat(subscription.isActive()).isFalse();
        assertThat(feedback.getDeletedAt()).isNotNull();

        verify(jwtProvider).revokeAllRefreshTokens(TEST_EMAIL);
        verify(emailVerificationService).clearVerificationState(1L, TEST_EMAIL, NOTIFY_EMAIL);
        verify(userRepository).saveAndFlush(user);
        verify(subscriptionRepository).findByUserId(1L);
        verify(userDeviceRepository, times(1)).deleteAllByUserId(1L);
        verify(feedbackRepository).findAllByUserId(1L);
        verify(subscriptionRepository, never()).deleteAllByUserId(1L);
        verify(courseReviewRepository, never()).deleteAllByUserId(1L);
        verify(courseEmojiReviewRepository, never()).deleteAllByUserId(1L);
        verify(notificationHistoryRepository, never()).deleteAllByUserId(1L);
        verify(feedbackRepository, never()).deleteAllByUserId(1L);
        verify(fileUploadService, never()).deleteFilesAfterTransactionCommit(org.mockito.ArgumentMatchers.anyList());
        verify(wishlistRepository, never()).deleteAllByUserId(1L);
        verify(timetableRepository, never()).delete(org.mockito.ArgumentMatchers.any());
        verify(userRepository, never()).delete(user);
    }

    @Test
    @DisplayName("동시 수정 충돌이면 외부 인증 폐기 전에 탈퇴를 중단한다")
    void withdraw_optimisticLockConflictStopsBeforeExternalRevocation() {
        User user = User.builder()
                .id(1L)
                .email(TEST_EMAIL)
                .name(NAME)
                .role(Role.USER)
                .build();
        securityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user))
                .thenThrow(new ObjectOptimisticLockingFailureException(User.class, 1L));

        assertThatThrownBy(userService::withdraw)
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        verify(jwtProvider, never()).revokeAllRefreshTokens(TEST_EMAIL);
        verify(emailVerificationService, never()).clearVerificationState(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
        verify(userDeviceRepository, never()).deleteAllByUserId(1L);
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
