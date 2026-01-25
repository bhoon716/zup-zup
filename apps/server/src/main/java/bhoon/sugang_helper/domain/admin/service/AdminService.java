package bhoon.sugang_helper.domain.admin.service;

import bhoon.sugang_helper.domain.admin.response.AdminDashboardResponse;
import bhoon.sugang_helper.domain.notification.repository.NotificationHistoryRepository;
import bhoon.sugang_helper.domain.subscription.repository.SubscriptionRepository;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final NotificationHistoryRepository notificationHistoryRepository;

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
}
