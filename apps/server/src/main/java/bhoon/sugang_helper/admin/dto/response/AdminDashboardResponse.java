package bhoon.sugang_helper.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminDashboardResponse {
    private long totalUsers;

    private long totalActiveSubscriptions;

    private long todayNotificationCount;

    private String crawlingStatus;

    private String lastCrawledAt;
}
