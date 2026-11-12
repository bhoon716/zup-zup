package bhoon.sugang_helper.admin.service;

import bhoon.sugang_helper.academic.api.operations.CrawlerAdminSnapshot;
import bhoon.sugang_helper.academic.api.operations.CrawlerRuns;
import bhoon.sugang_helper.academic.api.operations.CrawlerTargets;
import bhoon.sugang_helper.admin.dto.response.AdminDashboardSnapshotResponse;
import bhoon.sugang_helper.admin.dto.response.AdminOverviewResponse;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.api.security.SecurityUtil;
import bhoon.sugang_helper.identity.api.notifications.NotificationRecipient;
import bhoon.sugang_helper.identity.api.notifications.NotificationRecipients;
import bhoon.sugang_helper.identity.api.users.UserStatistics;
import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.api.NotificationOperations;
import bhoon.sugang_helper.seatalert.api.Subscriptions;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private static final long CRAWLER_DELAY_MINUTES = 15L;
    private static final String JBNU_LATENCY_METRIC = "crawler.upstream.latency";
    private final UserStatistics userStatistics;
    private final NotificationRecipients userNotificationQuery;
    private final Subscriptions subscriptionQueryService;
    private final NotificationOperations notificationTestSender;
    private final CrawlerTargets crawlerAdminSnapshotQuery;
    private final CrawlerRuns crawlerStatusQuery;
    private final MeterRegistry meterRegistry;

    private AdminOverviewResponse getDashboardOverview(CrawlerAdminSnapshot crawlTarget) {
        LocalDateTime now = LocalDateTime.now();
        long totalUsers = userStatistics.countUsers();
        long totalActiveSubscriptions = subscriptionQueryService.countActiveSubscriptions(
                crawlTarget.year(), crawlTarget.semester());
        LocalDateTime lastCrawledAt = crawlerStatusQuery.findLastSuccessAt().orElse(null);

        return AdminOverviewResponse.of(
                totalUsers,
                totalActiveSubscriptions,
                resolveCrawlerStatus(now, lastCrawledAt),
                lastCrawledAt,
                findJbnuLatencyMs(),
                now);
    }

    public AdminDashboardSnapshotResponse getDashboardSnapshot() {
        CrawlerAdminSnapshot crawlTarget = crawlerAdminSnapshotQuery.findCurrentAdminSnapshot();
        AdminOverviewResponse overview = getDashboardOverview(crawlTarget);

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

    private Long findJbnuLatencyMs() {
        Timer timer = meterRegistry.find(JBNU_LATENCY_METRIC).timer();
        if (timer == null || timer.count() == 0) {
            return null;
        }
        return Math.round(timer.mean(TimeUnit.MILLISECONDS));
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
