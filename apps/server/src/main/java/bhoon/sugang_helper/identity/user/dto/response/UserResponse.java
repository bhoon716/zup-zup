package bhoon.sugang_helper.identity.user.dto.response;

import bhoon.sugang_helper.identity.user.dto.UserOnboardingResult;
import bhoon.sugang_helper.identity.user.dto.UserProfileResult;
import bhoon.sugang_helper.identity.user.dto.UserProfileUpdateResult;
import bhoon.sugang_helper.identity.user.dto.UserSettingsResult;
import bhoon.sugang_helper.identity.user.entity.User;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserResponse {
    private Long id;

    private String email;

    private String name;

    private String role;

    private String notificationEmail;

    private boolean emailEnabled;

    private boolean webPushEnabled;

    private boolean fcmEnabled;

    private boolean onboardingCompleted;

    private String discordId;

    private boolean discordEnabled;

    public static UserResponse from(User user) {
        return fromValues(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                user.getNotificationEmail(),
                user.isEmailEnabled(),
                user.isWebPushEnabled(),
                user.isFcmEnabled(),
                user.isOnboardingCompleted(),
                user.getDiscordId(),
                user.isDiscordEnabled());
    }

    public static UserResponse from(UserProfileResult result) {
        return fromValues(
                result.id(),
                result.email(),
                result.name(),
                result.role(),
                result.notificationEmail(),
                result.emailEnabled(),
                result.webPushEnabled(),
                result.fcmEnabled(),
                result.onboardingCompleted(),
                result.discordId(),
                result.discordEnabled());
    }

    public static UserResponse from(UserProfileUpdateResult result) {
        return fromValues(
                result.id(),
                result.email(),
                result.name(),
                result.role(),
                result.notificationEmail(),
                result.emailEnabled(),
                result.webPushEnabled(),
                result.fcmEnabled(),
                result.onboardingCompleted(),
                result.discordId(),
                result.discordEnabled());
    }

    public static UserResponse from(UserSettingsResult result) {
        return fromValues(
                result.id(),
                result.email(),
                result.name(),
                result.role(),
                result.notificationEmail(),
                result.emailEnabled(),
                result.webPushEnabled(),
                result.fcmEnabled(),
                result.onboardingCompleted(),
                result.discordId(),
                result.discordEnabled());
    }

    public static UserResponse from(UserOnboardingResult result) {
        return fromValues(
                result.id(),
                result.email(),
                result.name(),
                result.role(),
                result.notificationEmail(),
                result.emailEnabled(),
                result.webPushEnabled(),
                result.fcmEnabled(),
                result.onboardingCompleted(),
                result.discordId(),
                result.discordEnabled());
    }

    private static UserResponse fromValues(
            Long id,
            String email,
            String name,
            String role,
            String notificationEmail,
            boolean emailEnabled,
            boolean webPushEnabled,
            boolean fcmEnabled,
            boolean onboardingCompleted,
            String discordId,
            boolean discordEnabled) {
        return UserResponse.builder()
                .id(id)
                .email(email)
                .name(name)
                .role(role)
                .notificationEmail(notificationEmail)
                .emailEnabled(emailEnabled)
                .webPushEnabled(webPushEnabled)
                .fcmEnabled(fcmEnabled)
                .discordEnabled(discordEnabled)
                .discordId(discordId)
                .onboardingCompleted(onboardingCompleted)
                .build();
    }
}
