package bhoon.sugang_helper.admin.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminOverviewResponse {

    private long totalUsers;

    private long totalActiveSubscriptions;

    private long todayNotificationCount;

    private String crawlingStatus;

    private LocalDateTime lastCrawledAt;

    private Long jbnuLatencyMs;

    private LocalDateTime serverTime;

    private List<AdminHourlyTrafficResponse> notificationTraffic;

    private List<AdminRecentLogResponse> recentLogs;
}
