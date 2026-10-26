package bhoon.sugang_helper.identity.user.dto.result;

public record UserDashboardProfile(
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
}
