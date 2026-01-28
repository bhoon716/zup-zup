package bhoon.sugang_helper.domain.admin.service;

import bhoon.sugang_helper.domain.admin.request.TestNotificationRequest;
import bhoon.sugang_helper.domain.admin.response.AdminDashboardResponse;
import bhoon.sugang_helper.domain.notification.repository.NotificationHistoryRepository;
import bhoon.sugang_helper.domain.notification.service.NotificationService;
import bhoon.sugang_helper.domain.subscription.repository.SubscriptionRepository;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private NotificationHistoryRepository notificationHistoryRepository;

    @Mock
    private NotificationService notificationService;

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

    @Test
    @DisplayName("테스트 알림 전송이 성공한다")
    void sendTestNotification_Success() {
        // given
        String email = "test@example.com";
        TestNotificationRequest request = new TestNotificationRequest();
        request.setEmail(email);
        request.setChannels(List.of("EMAIL", "WEB"));

        User user = User.builder()
                .email(email)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // when
        adminService.sendTestNotification(request);

        // then
        verify(notificationService).sendTestNotification(eq(user), any());
    }

    @Test
    @DisplayName("사용자 미존재 시 테스트 알림 전송이 실패한다")
    void sendTestNotification_UserNotFound() {
        // given
        String email = "notfound@example.com";
        TestNotificationRequest request = new TestNotificationRequest();
        request.setEmail(email);
        request.setChannels(List.of("EMAIL"));

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminService.sendTestNotification(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("해당 이메일의 사용자를 찾을 수 없습니다.");
    }
}
