package bhoon.sugang_helper.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.academic.crawling.dto.response.CrawlerAdminSnapshot;
import bhoon.sugang_helper.academic.crawling.service.CrawlerAdminSnapshotQuery;
import bhoon.sugang_helper.academic.crawling.service.CrawlerStatusQuery;
import bhoon.sugang_helper.admin.dto.response.AdminDashboardResponse;
import bhoon.sugang_helper.admin.dto.response.AdminDashboardSnapshotResponse;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.identity.user.dto.result.NotificationRecipient;
import bhoon.sugang_helper.identity.user.service.UserIdentityQuery;
import bhoon.sugang_helper.identity.user.service.UserNotificationQuery;
import bhoon.sugang_helper.seatalert.enums.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.service.NotificationHistoryQuery;
import bhoon.sugang_helper.seatalert.notification.service.NotificationTestSender;
import bhoon.sugang_helper.seatalert.subscription.service.SubscriptionQueryService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserIdentityQuery userIdentityQuery;
    @Mock
    private UserNotificationQuery userNotificationQuery;
    @Mock
    private SubscriptionQueryService subscriptionQueryService;
    @Mock
    private NotificationHistoryQuery notificationHistoryQuery;
    @Mock
    private NotificationTestSender notificationTestSender;
    @Mock
    private CrawlerAdminSnapshotQuery crawlerAdminSnapshotQuery;
    @Mock
    private CrawlerStatusQuery crawlerStatusQuery;

    @InjectMocks
    private AdminService adminService;

    @Test
    @DisplayName("관리자 대시보드 통계를 정상적으로 조회한다")
    void getDashboardStats() {
        when(userIdentityQuery.countUsers()).thenReturn(100L);
        when(subscriptionQueryService.countActiveSubscriptions()).thenReturn(50L);
        when(notificationHistoryQuery.countCreatedAfter(any(LocalDateTime.class))).thenReturn(20L);
        when(crawlerStatusQuery.findLastSuccessAt()).thenReturn(Optional.of(LocalDateTime.now()));

        AdminDashboardResponse result = adminService.getDashboardStats();

        assertThat(result.getTotalUsers()).isEqualTo(100L);
        assertThat(result.getTotalActiveSubscriptions()).isEqualTo(50L);
        assertThat(result.getTodayNotificationCount()).isEqualTo(20L);
        assertThat(result.getCrawlingStatus()).isEqualTo("RUNNING");
    }

    @Test
    @DisplayName("관리자 대시보드 스냅샷을 한 번에 조회한다")
    void getDashboardSnapshot() {
        when(userIdentityQuery.countUsers()).thenReturn(100L);
        when(subscriptionQueryService.countActiveSubscriptions()).thenReturn(50L);
        when(notificationHistoryQuery.countCreatedAfter(any(LocalDateTime.class))).thenReturn(20L);
        when(notificationHistoryQuery.findCreatedAtAfter(any(LocalDateTime.class))).thenReturn(List.of());
        when(notificationHistoryQuery.findLatest()).thenReturn(List.of());
        when(crawlerStatusQuery.findLastSuccessAt()).thenReturn(Optional.of(LocalDateTime.now()));
        when(crawlerAdminSnapshotQuery.findCurrentAdminSnapshot())
                .thenReturn(new CrawlerAdminSnapshot("2026", "U211600020"));

        AdminDashboardSnapshotResponse result = adminService.getDashboardSnapshot();

        assertThat(result.getOverview().getTotalUsers()).isEqualTo(100L);
        assertThat(result.getOverview().getTotalActiveSubscriptions()).isEqualTo(50L);
        assertThat(result.getCrawlTarget().year()).isEqualTo("2026");
        assertThat(result.getCrawlTarget().semester()).isEqualTo("U211600020");
    }

    @Test
    @DisplayName("테스트 알림 전송이 성공한다")
    void sendTestNotification_Success() {
        String email = "test@example.com";
        NotificationRecipient recipient = new NotificationRecipient(
                7L, false, email, email, true, true, false, false, null);

        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn(email);
            when(userNotificationQuery.findByEmail(email)).thenReturn(Optional.of(recipient));

            adminService.sendTestNotification();

            verify(notificationTestSender).sendAdminTestNotification(
                    eq(7L), eq(List.of(NotificationChannel.EMAIL, NotificationChannel.WEB)));
        }
    }

    @Test
    @DisplayName("사용자 미존재 시 테스트 알림 전송이 실패한다")
    void sendTestNotification_UserNotFound() {
        String email = "notfound@example.com";

        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn(email);
            when(userNotificationQuery.findByEmail(email)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.sendTestNotification())
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
        }
    }
}
