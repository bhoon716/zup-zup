package bhoon.sugang_helper.user.application.result;

import bhoon.sugang_helper.user.domain.User;

public record UserProfileResult(
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

    public static UserProfileResult from(User user) {
        return new UserProfileResult(
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
}
