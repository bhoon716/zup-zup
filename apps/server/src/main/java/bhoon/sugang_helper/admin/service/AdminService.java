package bhoon.sugang_helper.admin.service;

import bhoon.sugang_helper.academic.crawling.dto.response.CrawlerAdminSnapshot;
import bhoon.sugang_helper.academic.crawling.service.CourseCrawlerTargetService;
import bhoon.sugang_helper.academic.crawling.service.CrawlerRunStateService;
import bhoon.sugang_helper.admin.dto.response.AdminDashboardResponse;
import bhoon.sugang_helper.admin.dto.response.AdminDashboardSnapshotResponse;
import bhoon.sugang_helper.admin.dto.response.AdminHourlyTrafficResponse;
import bhoon.sugang_helper.admin.dto.response.AdminOverviewResponse;
import bhoon.sugang_helper.admin.dto.response.AdminRecentLogResponse;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.identity.user.dto.result.NotificationRecipient;
import bhoon.sugang_helper.identity.user.service.UserIdentityQueryService;
import bhoon.sugang_helper.identity.user.service.UserNotificationQueryService;
import bhoon.sugang_helper.seatalert.enums.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.dto.result.NotificationHistoryItem;
import bhoon.sugang_helper.seatalert.notification.service.NotificationHistoryQueryService;
import bhoon.sugang_helper.seatalert.notification.service.NotificationService;
import bhoon.sugang_helper.seatalert.subscription.service.SubscriptionQueryService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    private static final DateTimeFormatter HOUR_LABEL_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final long CRAWLER_DELAY_MINUTES = 15L;
    private final UserIdentityQueryService userIdentityQuery;
    private final UserNotificationQueryService userNotificationQuery;
    private final SubscriptionQueryService subscriptionQueryService;
    private final NotificationHistoryQueryService notificationHistoryQuery;
    private final NotificationService notificationTestSender;
    private final CourseCrawlerTargetService crawlerAdminSnapshotQuery;
    private final CrawlerRunStateService crawlerStatusQuery;




    public AdminDashboardResponse getDashboardStats() {
        long totalUsers = userIdentityQuery.countUsers();
        long totalActiveSubscriptions = subscriptionQueryService.countActiveSubscriptions();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = LocalDateTime.of(now.toLocalDate(), LocalTime.MIN);
        long todayNotifications = notificationHistoryQuery.countCreatedAfter(startOfToday);
        LocalDateTime lastCrawledAt = crawlerStatusQuery.findLastSuccessAt().orElse(null);

        return AdminDashboardResponse.builder().totalUsers(totalUsers)
                .totalActiveSubscriptions(totalActiveSubscriptions).todayNotificationCount(todayNotifications)
                .crawlingStatus(resolveCrawlerStatus(now, lastCrawledAt))
                .lastCrawledAt(lastCrawledAt != null ? lastCrawledAt.toString() : null)
                .build();
    }




    public AdminOverviewResponse getDashboardOverview() {
        LocalDateTime now = LocalDateTime.now();
        long totalUsers = userIdentityQuery.countUsers();
        long totalActiveSubscriptions = subscriptionQueryService.countActiveSubscriptions();
        LocalDateTime startOfToday = LocalDateTime.of(now.toLocalDate(), LocalTime.MIN);
        long todayNotifications = notificationHistoryQuery.countCreatedAfter(startOfToday);
        LocalDateTime lastCrawledAt = crawlerStatusQuery.findLastSuccessAt().orElse(null);

        return AdminOverviewResponse.builder()
                .totalUsers(totalUsers)
                .totalActiveSubscriptions(totalActiveSubscriptions)
                .todayNotificationCount(todayNotifications)
                .crawlingStatus(resolveCrawlerStatus(now, lastCrawledAt))
                .lastCrawledAt(lastCrawledAt)
                .jbnuLatencyMs(null)
                .serverTime(now)
                .notificationTraffic(buildHourlyNotificationTraffic(now))
                .recentLogs(buildRecentLogs(now, lastCrawledAt))
                .build();
    }




    public AdminDashboardSnapshotResponse getDashboardSnapshot() {
        AdminOverviewResponse overview = getDashboardOverview();
        CrawlerAdminSnapshot crawlTarget = crawlerAdminSnapshotQuery.findCurrentAdminSnapshot();

        return AdminDashboardSnapshotResponse.builder()
                .overview(overview)
                .crawlTarget(crawlTarget)
                .build();
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
        LocalDateTime startHour = now.minusHours(23)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        List<LocalDateTime> histories = notificationHistoryQuery.findCreatedAtAfter(startHour);
        long[] hourlyCounts = new long[24];

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
                .mapToObj(i -> AdminHourlyTrafficResponse.builder()
                        .label(startHour.plusHours(i).format(HOUR_LABEL_FORMATTER))
                        .count(hourlyCounts[i])
                        .build())
                .toList();
    }




    private List<AdminRecentLogResponse> buildRecentLogs(LocalDateTime now, LocalDateTime lastCrawledAt) {
        List<AdminRecentLogResponse> notificationLogs = notificationHistoryQuery.findLatest()
                .stream()
                .map(this::toNotificationLog)
                .toList();

        List<AdminRecentLogResponse> allLogs = new ArrayList<>(notificationLogs);
        allLogs.add(buildCrawlerLog(now, lastCrawledAt));

        return allLogs.stream()
                .sorted((a, b) -> {
                    if (a.getTimestamp() == null && b.getTimestamp() == null) {
                        return 0;
                    }
                    if (a.getTimestamp() == null) {
                        return 1;
                    }
                    if (b.getTimestamp() == null) {
                        return -1;
                    }
                    return b.getTimestamp().compareTo(a.getTimestamp());
                })
                .limit(20)
                .collect(Collectors.toList());
    }




    private AdminRecentLogResponse toNotificationLog(NotificationHistoryItem history) {
        String message = String.format("알림 발송 완료: %s (%s)", history.title(), history.channel().name());
        return AdminRecentLogResponse.builder()
                .timestamp(history.createdAt())
                .level("INFO")
                .message(message)
                .source("NotificationService")
                .build();
    }




    private AdminRecentLogResponse buildCrawlerLog(LocalDateTime now, LocalDateTime lastCrawledAt) {
        if (lastCrawledAt == null) {
            return AdminRecentLogResponse.builder()
                    .timestamp(now)
                    .level("WARN")
                    .message("크롤링 이력이 없어 상태를 확인할 수 없습니다.")
                    .source("CourseCrawler")
                    .build();
        }

        if (now.minusMinutes(CRAWLER_DELAY_MINUTES).isAfter(lastCrawledAt)) {
            return AdminRecentLogResponse.builder()
                    .timestamp(now)
                    .level("WARN")
                    .message("크롤러 실행 지연이 감지되었습니다.")
                    .source("CourseCrawler")
                    .build();
        }

        return AdminRecentLogResponse.builder()
                .timestamp(lastCrawledAt)
                .level("INFO")
                .message("크롤러 주기가 정상 동작 중입니다.")
                .source("CourseCrawler")
                .build();
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
        if (user.fcmEnabled()) {
            channels.add(NotificationChannel.FCM);
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
