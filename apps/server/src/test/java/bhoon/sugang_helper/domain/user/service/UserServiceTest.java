package bhoon.sugang_helper.domain.user.service;

import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.domain.subscription.repository.SubscriptionRepository;
import bhoon.sugang_helper.domain.user.entity.Role;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import bhoon.sugang_helper.domain.user.response.UserResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private EmailVerificationService emailVerificationService;

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
                .email("test@example.com")
                .name("Old Name")
                .role(Role.USER)
                .build();

        securityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // when
        UserResponse result = userService.updateProfile("New Name");

        // then
        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(user.getName()).isEqualTo("New Name");
    }

    @Test
    @DisplayName("회원 탈퇴 시 구독 정보를 포함하여 삭제한다")
    void withdraw() {
        // given
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Name")
                .role(Role.USER)
                .build();

        securityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // when
        userService.withdraw();

        // then
        verify(subscriptionRepository, times(1)).deleteAllByUserId(1L);
        verify(userRepository, times(1)).delete(user);
    }

    @Test
    @DisplayName("내 프로필 정보를 조회한다")
    void getMyProfile() {
        // given
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .role(Role.USER)
                .build();

        securityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // when
        UserResponse result = userService.getMyProfile();

        // then
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("사용자 정보를 찾을 수 없으면 예외를 발생시킨다")
    void getMyProfile_UserNotFound_ThrowsException() {
        // given
        securityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

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
                .email("test@example.com")
                .name("Tester")
                .role(Role.USER)
                .build();

        bhoon.sugang_helper.domain.user.request.UserSettingsRequest request = new bhoon.sugang_helper.domain.user.request.UserSettingsRequest(
                "new@example.com", true, true, false, true);

        securityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(emailVerificationService.isVerified(1L, "new@example.com")).thenReturn(true);

        // when
        UserResponse result = userService.updateSettings(request);

        // then
        assertThat(result.getNotificationEmail()).isEqualTo("new@example.com");
        assertThat(result.isEmailEnabled()).isTrue();
        assertThat(result.isDiscordEnabled()).isTrue();
        assertThat(user.getNotificationEmail()).isEqualTo("new@example.com");
        assertThat(user.isDiscordEnabled()).isTrue();
    }

    @Test
    @DisplayName("온보딩을 정상적으로 완료한다")
    void completeOnboarding() {
        // given
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Tester")
                .role(Role.USER)
                .onboardingCompleted(false)
                .build();

        bhoon.sugang_helper.domain.user.request.OnboardingRequest request = mock(
                bhoon.sugang_helper.domain.user.request.OnboardingRequest.class);
        when(request.getNotificationEmail()).thenReturn("notify@example.com");
        when(request.isEmailEnabled()).thenReturn(true);
        when(request.isWebPushEnabled()).thenReturn(true);

        securityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(emailVerificationService.isVerified(1L, "notify@example.com")).thenReturn(true);

        // when
        UserResponse result = userService.completeOnboarding(request);

        // then
        assertThat(result.isOnboardingCompleted()).isTrue();
        assertThat(user.isOnboardingCompleted()).isTrue();
        assertThat(user.getNotificationEmail()).isEqualTo("notify@example.com");
    }
}
