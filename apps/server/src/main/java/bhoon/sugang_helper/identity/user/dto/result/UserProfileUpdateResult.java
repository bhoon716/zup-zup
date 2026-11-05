package bhoon.sugang_helper.identity.user.dto.result;

import bhoon.sugang_helper.identity.user.entity.User;

public record UserProfileUpdateResult(
        Long id,
        String email,
        String name,
        String role,
        String notificationEmail,
        boolean emailEnabled,
        boolean webPushEnabled,
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
                user.isOnboardingCompleted(),
                user.getDiscordId(),
                user.isDiscordEnabled());
    }
}
