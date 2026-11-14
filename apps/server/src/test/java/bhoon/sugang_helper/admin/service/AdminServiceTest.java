package bhoon.sugang_helper.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.academic.api.operations.CrawlerAdminSnapshot;
import bhoon.sugang_helper.academic.crawling.service.CourseCrawlerTargetService;
import bhoon.sugang_helper.academic.crawling.service.CrawlerRunStateService;
import bhoon.sugang_helper.admin.dto.response.AdminDashboardSnapshotResponse;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.api.security.SecurityUtil;
import bhoon.sugang_helper.identity.api.notifications.NotificationRecipient;
import bhoon.sugang_helper.identity.user.service.UserIdentityQueryService;
import bhoon.sugang_helper.identity.user.service.UserNotificationQueryService;
import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.service.NotificationService;
import bhoon.sugang_helper.seatalert.subscription.service.SubscriptionQueryService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserIdentityQueryService userIdentityQuery;

    @Mock
    private UserNotificationQueryService userNotificationQuery;

    @Mock
    private SubscriptionQueryService subscriptionQueryService;

    @Mock
    private NotificationService notificationTestSender;

    @Mock
    private CourseCrawlerTargetService crawlerAdminSnapshotQuery;

    @Mock
    private CrawlerRunStateService crawlerStatusQuery;

    @InjectMocks
    private AdminService adminService;

    @ParameterizedTest
    @CsvSource(value = {
            "없음|UNKNOWN",
            "30|DEGRADED",
            "0|RUNNING"
    }, delimiter = '|', nullValues = "없음")
    @DisplayName("관리자 대시보드는 수집 대상 학기의 구독 수와 크롤러 상태를 반환한다")
    void getDashboardSnapshot(Long minutesAgo, String status) {
        // given
        LocalDateTime lastCrawledAt = minutesAgo == null ? null : LocalDateTime.now().minusMinutes(minutesAgo);
        CrawlerAdminSnapshot crawlTarget = new CrawlerAdminSnapshot("2026", "U211600020");
        when(userIdentityQuery.countUsers()).thenReturn(100L);
        when(subscriptionQueryService.countActiveSubscriptions(crawlTarget.year(), crawlTarget.semester()))
                .thenReturn(50L);
        when(crawlerStatusQuery.findLastSuccessAt()).thenReturn(Optional.ofNullable(lastCrawledAt));
        when(crawlerAdminSnapshotQuery.findCurrentAdminSnapshot()).thenReturn(crawlTarget);

        // when
        AdminDashboardSnapshotResponse result = adminService.getDashboardSnapshot();

        // then
        assertThat(result.getOverview().getTotalUsers()).isEqualTo(100L);
        assertThat(result.getOverview().getTotalActiveSubscriptions()).isEqualTo(50L);
        assertThat(result.getOverview().getCrawlingStatus()).isEqualTo(status);
        assertThat(result.getOverview().getLastCrawledAt()).isEqualTo(lastCrawledAt);
        assertThat(result.getCrawlTarget()).isEqualTo(crawlTarget);
    }

    @Test
    @DisplayName("테스트 알림 전송이 성공한다")
    void sendTestNotification_Success() {
        // given
        String email = "test@example.com";
        NotificationRecipient recipient = new NotificationRecipient(
                7L, false, email, email, true, true, false, null);

        // when & then
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
        // given
        String email = "notfound@example.com";

        // when & then
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn(email);
            when(userNotificationQuery.findByEmail(email)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.sendTestNotification())
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
        }
    }
}
