package bhoon.sugang_helper.admin.dto.response;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminOverviewResponse {

    private final long totalUsers;

    private final long totalActiveSubscriptions;

    private final String crawlingStatus;

    private final LocalDateTime lastCrawledAt;

    private final Long jbnuLatencyMs;

    private final LocalDateTime serverTime;

    public static AdminOverviewResponse of(long totalUsers, long totalActiveSubscriptions,
                                           String crawlingStatus, LocalDateTime lastCrawledAt, Long jbnuLatencyMs,
                                           LocalDateTime serverTime) {
        return new AdminOverviewResponse(totalUsers, totalActiveSubscriptions, crawlingStatus, lastCrawledAt,
                jbnuLatencyMs, serverTime);
    }
}
