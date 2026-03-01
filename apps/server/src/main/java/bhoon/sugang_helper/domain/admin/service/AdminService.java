package bhoon.sugang_helper.domain.admin.service;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.domain.admin.response.AdminDashboardResponse;
import bhoon.sugang_helper.domain.admin.response.AdminHourlyTrafficResponse;
import bhoon.sugang_helper.domain.admin.response.AdminOverviewResponse;
import bhoon.sugang_helper.domain.admin.response.AdminRecentLogResponse;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import bhoon.sugang_helper.domain.notification.entity.NotificationHistory;
import bhoon.sugang_helper.domain.notification.repository.NotificationHistoryRepository;
import bhoon.sugang_helper.domain.notification.sender.NotificationChannel;
import bhoon.sugang_helper.domain.notification.service.NotificationService;
import bhoon.sugang_helper.domain.subscription.repository.SubscriptionRepository;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import bhoon.sugang_helper.common.util.SecurityUtil;
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

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final NotificationService notificationService;

    private static final DateTimeFormatter HOUR_LABEL_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final long CRAWLER_DELAY_MINUTES = 15L;

    /**
     * 관리자 대시보드 기본 통계 정보를 조회합니다.
     */
    public AdminDashboardResponse getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalActiveSubscriptions = subscriptionRepository.countByIsActiveTrue();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = LocalDateTime.of(now.toLocalDate(), LocalTime.MIN);
        long todayNotifications = notificationHistoryRepository.countByCreatedAtAfter(startOfToday);
        LocalDateTime lastCrawledAt = courseRepository.findMaxLastCrawledAt().orElse(null);

        return AdminDashboardResponse.builder().totalUsers(totalUsers)
                .totalActiveSubscriptions(totalActiveSubscriptions).todayNotificationCount(todayNotifications)
                .crawlingStatus(resolveCrawlerStatus(now, lastCrawledAt))
                .lastCrawledAt(lastCrawledAt != null ? lastCrawledAt.toString() : null)
                .build();
    }

    /**
     * 관리자 대시보드 시스템 현황 및 트래픽 정보를 포함한 전체 개요를 조회합니다.
     */
    public AdminOverviewResponse getDashboardOverview() {
        LocalDateTime now = LocalDateTime.now();
        long totalUsers = userRepository.count();
        long totalActiveSubscriptions = subscriptionRepository.countByIsActiveTrue();
        LocalDateTime startOfToday = LocalDateTime.of(now.toLocalDate(), LocalTime.MIN);
        long todayNotifications = notificationHistoryRepository.countByCreatedAtAfter(startOfToday);
        LocalDateTime lastCrawledAt = courseRepository.findMaxLastCrawledAt().orElse(null);

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

    /**
     * 현재 시간을 기준으로 크롤러의 동작 상태를 판단합니다.
     */
    private String resolveCrawlerStatus(LocalDateTime now, LocalDateTime lastCrawledAt) {
        if (lastCrawledAt == null) {
            return "UNKNOWN";
        }
        if (now.minusMinutes(CRAWLER_DELAY_MINUTES).isAfter(lastCrawledAt)) {
            return "DEGRADED";
        }
        return "RUNNING";
    }

    /**
     * 최근 24시간 동안의 시간별 알림 발송 트래픽 통계를 생성합니다.
     */
    private List<AdminHourlyTrafficResponse> buildHourlyNotificationTraffic(LocalDateTime now) {
        LocalDateTime startHour = now.minusHours(23)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        List<NotificationHistory> histories = notificationHistoryRepository.findByCreatedAtAfter(startHour);
        long[] hourlyCounts = new long[24];

        for (NotificationHistory history : histories) {
            LocalDateTime createdAt = history.getCreatedAt();
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

    /**
     * 알림 기록 및 크롤러 상태를 기반으로 최근 시스템 로그 목록을 구성합니다.
     */
    private List<AdminRecentLogResponse> buildRecentLogs(LocalDateTime now, LocalDateTime lastCrawledAt) {
        List<AdminRecentLogResponse> notificationLogs = notificationHistoryRepository.findTop50ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toNotificationLog)
                .toList();

        List<AdminRecentLogResponse> allLogs = new ArrayList<>(notificationLogs);
        allLogs.add(buildCrawlerLog(now, lastCrawledAt));

        if (allLogs.isEmpty()) {
            allLogs.add(AdminRecentLogResponse.builder()
                    .timestamp(now)
                    .level("INFO")
                    .message("표시할 로그가 없습니다.")
                    .source("AdminService")
                    .build());
        }

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

    /**
     * 알림 이력 데이터를 로그 형태의 응답 객체로 변환합니다.
     */
    private AdminRecentLogResponse toNotificationLog(NotificationHistory history) {
        String message = String.format("알림 발송 완료: %s (%s)", history.getTitle(), history.getChannel().name());
        return AdminRecentLogResponse.builder()
                .timestamp(history.getCreatedAt())
                .level("INFO")
                .message(message)
                .source("NotificationService")
                .build();
    }

    /**
     * 크롤러의 최종 실행 이력을 바탕으로 크롤러 관련 로그를 생성합니다.
     */
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

    /**
     * 현재 관리자의 활성화된 알림 채널로 테스트 알림을 발송합니다.
     */
    @Transactional
    public void sendTestNotification() {
        String email = SecurityUtil.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "email: " + email));

        List<NotificationChannel> channels = new ArrayList<>();
        if (user.isEmailEnabled())
            channels.add(NotificationChannel.EMAIL);
        if (user.isFcmEnabled())
            channels.add(NotificationChannel.FCM);
        if (user.isWebPushEnabled())
            channels.add(NotificationChannel.WEB);
        if (user.isDiscordEnabled())
            channels.add(NotificationChannel.DISCORD);

        if (channels.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "활성화된 알림 채널이 없습니다. 설정에서 알림을 활성화해주세요.");
        }

        notificationService.sendTestNotification(user, channels);
    }
}
