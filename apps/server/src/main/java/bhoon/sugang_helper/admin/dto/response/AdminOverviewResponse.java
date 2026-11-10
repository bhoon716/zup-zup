package bhoon.sugang_helper.admin.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminOverviewResponse {

    private final long totalUsers;

    private final long totalActiveSubscriptions;

    private final long todayNotificationCount;

    private final String crawlingStatus;

    private final LocalDateTime lastCrawledAt;

    private final Long jbnuLatencyMs;

    private final LocalDateTime serverTime;

    private final List<AdminHourlyTrafficResponse> notificationTraffic;

    private final List<AdminRecentLogResponse> recentLogs;

    public static AdminOverviewResponse of(long totalUsers, long totalActiveSubscriptions, long todayNotificationCount,
                                           String crawlingStatus, LocalDateTime lastCrawledAt, Long jbnuLatencyMs,
                                           LocalDateTime serverTime, List<AdminHourlyTrafficResponse> notificationTraffic,
                                           List<AdminRecentLogResponse> recentLogs) {
        return new AdminOverviewResponse(totalUsers, totalActiveSubscriptions, todayNotificationCount, crawlingStatus,
                lastCrawledAt, jbnuLatencyMs, serverTime, notificationTraffic, recentLogs);
    }
}
