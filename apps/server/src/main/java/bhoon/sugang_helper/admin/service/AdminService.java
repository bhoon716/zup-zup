package bhoon.sugang_helper.admin.service;

import bhoon.sugang_helper.academic.api.operations.CrawlerAdminSnapshot;
import bhoon.sugang_helper.academic.api.operations.CrawlerRuns;
import bhoon.sugang_helper.academic.api.operations.CrawlerTargets;
import bhoon.sugang_helper.admin.dto.response.AdminDashboardSnapshotResponse;
import bhoon.sugang_helper.admin.dto.response.AdminHourlyTrafficResponse;
import bhoon.sugang_helper.admin.dto.response.AdminOverviewResponse;
import bhoon.sugang_helper.admin.dto.response.AdminRecentLogResponse;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.api.security.SecurityUtil;
import bhoon.sugang_helper.identity.api.notifications.NotificationRecipient;
import bhoon.sugang_helper.identity.api.notifications.NotificationRecipients;
import bhoon.sugang_helper.identity.api.users.UserStatistics;
import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.api.NotificationHistoryQueries;
import bhoon.sugang_helper.seatalert.api.NotificationOperations;
import bhoon.sugang_helper.seatalert.api.Subscriptions;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private static final long CRAWLER_DELAY_MINUTES = 15L;
    private static final int HOURLY_TRAFFIC_WINDOW_HOURS = 24;
    private static final int RECENT_LOG_LIMIT = 20;
    private final UserStatistics userStatistics;
    private final NotificationRecipients userNotificationQuery;
    private final Subscriptions subscriptionQueryService;
    private final NotificationHistoryQueries notificationHistoryQuery;
    private final NotificationOperations notificationTestSender;
    private final CrawlerTargets crawlerAdminSnapshotQuery;
    private final CrawlerRuns crawlerStatusQuery;

    private AdminOverviewResponse getDashboardOverview() {
        LocalDateTime now = LocalDateTime.now();
        long totalUsers = userStatistics.countUsers();
        long totalActiveSubscriptions = subscriptionQueryService.countActiveSubscriptions();
        LocalDateTime startOfToday = LocalDateTime.of(now.toLocalDate(), LocalTime.MIN);
        long todayNotifications = notificationHistoryQuery.countCreatedAfter(startOfToday);
        LocalDateTime lastCrawledAt = crawlerStatusQuery.findLastSuccessAt().orElse(null);

        return AdminOverviewResponse.of(
                totalUsers,
                totalActiveSubscriptions,
                todayNotifications,
                resolveCrawlerStatus(now, lastCrawledAt),
                lastCrawledAt,
                null,
                now,
                buildHourlyNotificationTraffic(now),
                buildRecentLogs(now, lastCrawledAt));
    }

    public AdminDashboardSnapshotResponse getDashboardSnapshot() {
        AdminOverviewResponse overview = getDashboardOverview();
        CrawlerAdminSnapshot crawlTarget = crawlerAdminSnapshotQuery.findCurrentAdminSnapshot();

        return AdminDashboardSnapshotResponse.of(overview, crawlTarget);
    }

    private String resolveCrawlerStatus(LocalDateTime now, LocalDateTime lastCrawledAt) {
        if (lastCrawledAt == null) {
            return "UNKNOWN";
        }
        if (now.minusMinutes(CRAWLER_DELAY_MINUTES).isAfter(lastCrawledAt)) {
            return "DEGRADED";
        }
        return "RUNNING";
    }

    private List<AdminHourlyTrafficResponse> buildHourlyNotificationTraffic(LocalDateTime now) {
        LocalDateTime startHour = now.minusHours(HOURLY_TRAFFIC_WINDOW_HOURS - 1)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        List<LocalDateTime> histories = notificationHistoryQuery.findCreatedAtAfter(startHour);
        long[] hourlyCounts = new long[HOURLY_TRAFFIC_WINDOW_HOURS];

        for (LocalDateTime createdAt : histories) {
            if (createdAt == null || createdAt.isBefore(startHour)) {
                continue;
            }

            long index = Duration.between(startHour, createdAt).toHours();
            if (index < 0 || index >= hourlyCounts.length) {
                continue;
            }
            hourlyCounts[(int) index]++;
        }

        return IntStream.range(0, hourlyCounts.length)
                .mapToObj(index -> AdminHourlyTrafficResponse.of(startHour.plusHours(index), hourlyCounts[index]))
                .toList();
    }

    private List<AdminRecentLogResponse> buildRecentLogs(LocalDateTime now, LocalDateTime lastCrawledAt) {
        List<AdminRecentLogResponse> notificationLogs = notificationHistoryQuery.findLatest()
                .stream()
                .map(AdminRecentLogResponse::from)
                .toList();

        List<AdminRecentLogResponse> allLogs = new ArrayList<>(notificationLogs);
        allLogs.add(buildCrawlerLog(now, lastCrawledAt));

        return allLogs.stream()
                .sorted(Comparator.comparing(AdminRecentLogResponse::getTimestamp,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(RECENT_LOG_LIMIT)
                .collect(Collectors.toList());
    }

    private AdminRecentLogResponse buildCrawlerLog(LocalDateTime now, LocalDateTime lastCrawledAt) {
        if (lastCrawledAt == null) {
            return AdminRecentLogResponse.crawlerHistoryMissing(now);
        }

        if (now.minusMinutes(CRAWLER_DELAY_MINUTES).isAfter(lastCrawledAt)) {
            return AdminRecentLogResponse.crawlerDelayed(now);
        }

        return AdminRecentLogResponse.crawlerHealthy(lastCrawledAt);
    }

    @Transactional
    public void sendTestNotification() {
        String email = SecurityUtil.getCurrentUserEmail();
        NotificationRecipient user = userNotificationQuery.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "email: " + email));

        List<NotificationChannel> channels = enabledChannels(user);

        if (channels.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "활성화된 알림 채널이 없습니다. 설정에서 알림을 활성화해주세요.");
        }

        notificationTestSender.sendAdminTestNotification(user.id(), channels);
    }

    private List<NotificationChannel> enabledChannels(NotificationRecipient user) {
        List<NotificationChannel> channels = new ArrayList<>();
        if (user.emailEnabled()) {
            channels.add(NotificationChannel.EMAIL);
        }
        if (user.webPushEnabled()) {
            channels.add(NotificationChannel.WEB);
        }
        if (user.discordEnabled()) {
            channels.add(NotificationChannel.DISCORD);
        }
        return channels;
    }
}
