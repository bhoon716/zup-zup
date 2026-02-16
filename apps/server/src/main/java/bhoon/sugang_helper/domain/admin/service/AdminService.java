package bhoon.sugang_helper.domain.admin.service;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.domain.admin.request.TestNotificationRequest;
import bhoon.sugang_helper.domain.admin.response.AdminDashboardResponse;
import bhoon.sugang_helper.domain.notification.repository.NotificationHistoryRepository;
import bhoon.sugang_helper.domain.notification.sender.NotificationChannel;
import bhoon.sugang_helper.domain.notification.service.NotificationService;
import bhoon.sugang_helper.domain.subscription.repository.SubscriptionRepository;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final NotificationService notificationService;

    public AdminDashboardResponse getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalActiveSubscriptions = subscriptionRepository.countByIsActiveTrue();

        LocalDateTime startOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        long todayNotifications = notificationHistoryRepository.countByCreatedAtAfter(startOfToday);

        return AdminDashboardResponse.builder()
                .totalUsers(totalUsers)
                .totalActiveSubscriptions(totalActiveSubscriptions)
                .todayNotificationCount(todayNotifications)
                .crawlingStatus("RUNNING") // 임시: 나중에 크롤러 상태 연동 필요
                .lastCrawledAt(LocalDateTime.now().toString()) // 임시
                .build();
    }

    @Transactional
    public void sendTestNotification(TestNotificationRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "email: " + request.getEmail()));

        List<NotificationChannel> channels = request.getChannels().stream()
                .map(NotificationChannel::valueOf)
                .toList();

        notificationService.sendTestNotification(user, channels);
    }
}
