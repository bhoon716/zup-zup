package bhoon.sugang_helper.dashboard.application.result;

import bhoon.sugang_helper.identity.user.contract.UserDashboardProfile;

public record DashboardUserResult(
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

    public static DashboardUserResult from(UserDashboardProfile user) {
        return new DashboardUserResult(
                user.id(),
                user.email(),
                user.name(),
                user.role(),
                user.notificationEmail(),
                user.emailEnabled(),
                user.webPushEnabled(),
                user.fcmEnabled(),
                user.onboardingCompleted(),
                user.discordId(),
                user.discordEnabled());
    }
}
