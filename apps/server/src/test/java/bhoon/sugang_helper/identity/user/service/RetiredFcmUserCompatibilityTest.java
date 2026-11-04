package bhoon.sugang_helper.identity.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
    void rejectsFcmSettingBeforeLookingUpOrVerifyingAnAddress() {
        // given
        UpdateSettingsCommand command = new UpdateSettingsCommand("new@example.invalid", true, false, true, false);

        // when & then
        assertThatThrownBy(() -> userService.updateSettings(command))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
        verifyNoInteractions(userRepository, deviceRepository, emailVerification);
    }

    @Test
    void rejectsFcmOnboardingBeforeLookingUpOrVerifyingAnAddress() {
        // given
        CompleteOnboardingCommand command = new CompleteOnboardingCommand(
                "new@example.invalid", true, false, true, false);

        // when & then
        assertThatThrownBy(() -> userService.completeOnboarding(command))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
        verifyNoInteractions(userRepository, deviceRepository, emailVerification);
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
}
