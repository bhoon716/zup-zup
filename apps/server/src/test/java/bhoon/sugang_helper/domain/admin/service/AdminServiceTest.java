package bhoon.sugang_helper.domain.admin.service;

import bhoon.sugang_helper.domain.admin.response.AdminDashboardResponse;
import bhoon.sugang_helper.domain.notification.repository.NotificationHistoryRepository;
import bhoon.sugang_helper.domain.subscription.repository.SubscriptionRepository;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private NotificationHistoryRepository notificationHistoryRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    @DisplayName("관리자 대시보드 통계를 정상적으로 조회한다")
    void getDashboardStats() {
        // given
        when(userRepository.count()).thenReturn(100L);
        when(subscriptionRepository.countByIsActiveTrue()).thenReturn(50L);
        when(notificationHistoryRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(20L);

        // when
        AdminDashboardResponse result = adminService.getDashboardStats();

        // then
        assertThat(result.getTotalUsers()).isEqualTo(100L);
        assertThat(result.getTotalActiveSubscriptions()).isEqualTo(50L);
        assertThat(result.getTodayNotificationCount()).isEqualTo(20L);
        assertThat(result.getCrawlingStatus()).isEqualTo("RUNNING");
    }
}
