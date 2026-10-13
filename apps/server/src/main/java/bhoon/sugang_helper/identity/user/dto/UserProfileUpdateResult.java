package bhoon.sugang_helper.identity.user.dto;

import bhoon.sugang_helper.identity.user.entity.User;

public record UserProfileUpdateResult(
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

    public static UserProfileUpdateResult from(User user) {
        return new UserProfileUpdateResult(
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
