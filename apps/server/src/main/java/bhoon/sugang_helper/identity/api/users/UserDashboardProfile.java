package bhoon.sugang_helper.identity.api.users;

public record UserDashboardProfile(
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
}
