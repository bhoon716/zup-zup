package bhoon.sugang_helper.identity.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.identity.auth.service.SessionInvalidator;
import bhoon.sugang_helper.identity.user.dto.command.CompleteOnboardingCommand;
import bhoon.sugang_helper.identity.user.dto.command.RegisterDeviceCommand;
import bhoon.sugang_helper.identity.user.dto.command.UpdateSettingsCommand;
import bhoon.sugang_helper.identity.user.dto.result.UserProfileResult;
import bhoon.sugang_helper.identity.user.entity.User;
import bhoon.sugang_helper.identity.user.enums.DeviceType;
import bhoon.sugang_helper.identity.user.enums.NotificationDeviceType;
import bhoon.sugang_helper.identity.user.enums.Role;
import bhoon.sugang_helper.identity.user.repository.UserDeviceRepository;
import bhoon.sugang_helper.identity.user.repository.UserRepository;
import bhoon.sugang_helper.seatalert.enums.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.service.NotificationService;
import bhoon.sugang_helper.seatalert.subscription.service.SubscriptionAccountLifecycleService;
import bhoon.sugang_helper.support.feedback.service.FeedbackAccountLifecycleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class RetiredFcmUserCompatibilityTest {

    @Mock private UserRepository userRepository;
    @Mock private UserDeviceRepository deviceRepository;
    @Mock private WebPushEndpointValidator endpointValidator;
    @Mock private SubscriptionAccountLifecycleService subscriptionLifecycle;
    @Mock private FeedbackAccountLifecycleService feedbackLifecycle;
    @Mock private EmailVerificationService emailVerification;
    @Mock private NotificationService notificationService;
    @Mock private SessionInvalidator sessionInvalidator;
    @InjectMocks private UserService userService;
    @InjectMocks private UserDeviceService deviceService;

    @Test
    void rejectsNewFcmSettingBeforeVerifyingAnAddress() {
        // given
        User current = User.builder().id(1L).email("current@example.invalid").fcmEnabled(false).build();
        UpdateSettingsCommand command = new UpdateSettingsCommand("new@example.invalid", true, false, true, false);

        // when & then
        try (MockedStatic<SecurityUtil> authentication = authenticate(current)) {
            assertThatThrownBy(() -> userService.updateSettings(command))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
        }
        verify(userRepository).findByEmail(current.getEmail());
        verifyNoInteractions(deviceRepository, emailVerification);
        assertThat(current.isFcmEnabled()).isFalse();
    }

    @Test
    void rejectsNewFcmOnboardingBeforeVerifyingAnAddress() {
        // given
        User current = User.builder().id(1L).email("current@example.invalid").fcmEnabled(false).build();
        CompleteOnboardingCommand command = new CompleteOnboardingCommand(
                "new@example.invalid", true, false, true, false);

        // when & then
        try (MockedStatic<SecurityUtil> authentication = authenticate(current)) {
            assertThatThrownBy(() -> userService.completeOnboarding(command))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
        }
        verify(userRepository).findByEmail(current.getEmail());
        verifyNoInteractions(deviceRepository, emailVerification);
        assertThat(current.isFcmEnabled()).isFalse();
    }

    @Test
    void rejectsNewFcmDeviceWithoutUpdatingAnyStoredDevice() {
        // given
        RegisterDeviceCommand command = new RegisterDeviceCommand(
                DeviceType.FCM, "legacy-token", null, null, "old app");

        // when & then
        assertThatThrownBy(() -> deviceService.registerDevice(command))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
        verifyNoInteractions(userRepository, deviceRepository, endpointValidator);
    }

    @Test
    void supportedSettingsPreserveTheHistoricalFcmFlagWithoutTreatingItAsActive() {
        // given
        User legacy = User.builder().id(1L).email("legacy@example.invalid").name("legacy")
                .role(Role.USER).fcmEnabled(true).build();

        // when
        legacy.updateSettings("legacy@example.invalid", false, false, false);
        legacy.completeOnboarding("legacy@example.invalid", false, false, false);

        // then
        assertThat(legacy.isFcmEnabled()).isTrue();
        assertThat(legacy.isOnboardingCompleted()).isTrue();
        assertThat(legacy.hasEnabledNotificationChannels()).isFalse();
    }

    @Test
    void historicalFcmTypesAndProfileFlagRemainReadable() throws Exception {
        // given
        User legacy = User.builder().id(1L).email("legacy@example.invalid").name("legacy")
                .role(Role.USER).fcmEnabled(true).build();
        ObjectMapper mapper = new ObjectMapper();

        // when & then
        assertThat(mapper.readValue("\"FCM\"", NotificationChannel.class)).isEqualTo(NotificationChannel.FCM);
        assertThat(DeviceType.valueOf("FCM")).isEqualTo(DeviceType.FCM);
        assertThat(NotificationDeviceType.valueOf("FCM")).isEqualTo(NotificationDeviceType.FCM);
        assertThat(mapper.readTree(mapper.writeValueAsString(UserProfileResult.from(legacy)))
                .path("fcmEnabled").asBoolean()).isTrue();
    }
    @Test
    void legacyTrueEchoCanSaveSupportedSettingsWithoutChangingTheHistoricalFlag() {
        // given
        User legacy = User.builder().id(1L).email("legacy@example.invalid").name("legacy")
                .role(Role.USER).fcmEnabled(true).build();
        UpdateSettingsCommand command = new UpdateSettingsCommand(legacy.getEmail(), true, true, true, false);

        // when
        try (MockedStatic<SecurityUtil> authentication = authenticate(legacy)) {
            var result = userService.updateSettings(command);

            // then
            assertThat(result.fcmEnabled()).isTrue();
            assertThat(result.emailEnabled()).isTrue();
            assertThat(result.webPushEnabled()).isTrue();
        }
        assertThat(legacy.isFcmEnabled()).isTrue();
        assertThat(legacy.isEmailEnabled()).isTrue();
        assertThat(legacy.isWebPushEnabled()).isTrue();
        verifyNoInteractions(deviceRepository, emailVerification);
    }

    @Test
    void legacyTrueEchoCanCompleteOnboardingWithoutReactivatingFcm() {
        // given
        User legacy = User.builder().id(1L).email("legacy@example.invalid").name("legacy")
                .role(Role.USER).fcmEnabled(true).build();
        CompleteOnboardingCommand command = new CompleteOnboardingCommand(
                legacy.getEmail(), true, false, true, false);

        // when
        try (MockedStatic<SecurityUtil> authentication = authenticate(legacy)) {
            var result = userService.completeOnboarding(command);

            // then
            assertThat(result.fcmEnabled()).isTrue();
            assertThat(result.emailEnabled()).isTrue();
            assertThat(result.onboardingCompleted()).isTrue();
        }
        assertThat(legacy.isFcmEnabled()).isTrue();
        assertThat(legacy.isOnboardingCompleted()).isTrue();
        verifyNoInteractions(deviceRepository, emailVerification);
    }

    private MockedStatic<SecurityUtil> authenticate(User user) {
        MockedStatic<SecurityUtil> authentication = mockStatic(SecurityUtil.class);
        authentication.when(SecurityUtil::getCurrentUserEmail).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        return authentication;
    }

}
