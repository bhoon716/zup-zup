package bhoon.sugang_helper.identity.user.dto.response;

import bhoon.sugang_helper.identity.user.dto.result.UserProfileResult;
import bhoon.sugang_helper.identity.user.dto.result.UserSettingsResult;
import bhoon.sugang_helper.identity.user.dto.result.UserOnboardingResult;
import bhoon.sugang_helper.identity.user.entity.User;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
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

    private boolean onboardingCompleted;

    private String discordId;

    private boolean discordEnabled;

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                user.getNotificationEmail(),
                user.isEmailEnabled(),
                user.isWebPushEnabled(),
                user.isOnboardingCompleted(),
                user.getDiscordId(),
                user.isDiscordEnabled());
    }

    public static UserResponse from(UserProfileResult result) {
        return new UserResponse(
                result.id(),
                result.email(),
                result.name(),
                result.role(),
                result.notificationEmail(),
                result.emailEnabled(),
                result.webPushEnabled(),
                result.onboardingCompleted(),
                result.discordId(),
                result.discordEnabled());
    }

    public static UserResponse from(UserSettingsResult result) {
        return new UserResponse(
                result.id(),
                result.email(),
                result.name(),
                result.role(),
                result.notificationEmail(),
                result.emailEnabled(),
                result.webPushEnabled(),
                result.onboardingCompleted(),
                result.discordId(),
                result.discordEnabled());
    }

    public static UserResponse from(UserOnboardingResult result) {
        return new UserResponse(
                result.id(),
                result.email(),
                result.name(),
                result.role(),
                result.notificationEmail(),
                result.emailEnabled(),
                result.webPushEnabled(),
                result.onboardingCompleted(),
                result.discordId(),
                result.discordEnabled());
    }
}
