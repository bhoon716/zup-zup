package bhoon.sugang_helper.dashboard.dto.response;

public record LandingStatsResponse(long totalUsers, long totalSentSeatNotifications) {

    public static LandingStatsResponse of(long totalUsers, long totalSentSeatNotifications) {
        return new LandingStatsResponse(totalUsers, totalSentSeatNotifications);
    }
}
