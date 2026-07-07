package bhoon.sugang_helper.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.admin.application.AdminDashboardResponse;
import bhoon.sugang_helper.admin.application.AdminDashboardSnapshotResponse;
import bhoon.sugang_helper.course.domain.CourseRepository;
import bhoon.sugang_helper.crawling.application.AdminCrawlTargetResponse;
import bhoon.sugang_helper.crawling.application.CourseCrawlerTargetService;
import bhoon.sugang_helper.notification.domain.NotificationHistoryRepository;
import bhoon.sugang_helper.notification.application.NotificationService;
import bhoon.sugang_helper.subscription.domain.SubscriptionRepository;
import bhoon.sugang_helper.user.domain.User;
import bhoon.sugang_helper.user.domain.UserRepository;
import java.time.LocalDateTime;
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
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private NotificationHistoryRepository notificationHistoryRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private CourseCrawlerTargetService courseCrawlerTargetService;

    @InjectMocks
    private AdminService adminService;

    @Test
    @DisplayName("관리자 대시보드 통계를 정상적으로 조회한다")
    void getDashboardStats() {
        // given
        when(userRepository.count()).thenReturn(100L);
        when(subscriptionRepository.countByIsActiveTrue()).thenReturn(50L);
        when(notificationHistoryRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(20L);
        when(courseRepository.findMaxLastCrawledAt()).thenReturn(Optional.of(LocalDateTime.now()));

        // when
        AdminDashboardResponse result = adminService.getDashboardStats();

        // then
        assertThat(result.getTotalUsers()).isEqualTo(100L);
        assertThat(result.getTotalActiveSubscriptions()).isEqualTo(50L);
        assertThat(result.getTodayNotificationCount()).isEqualTo(20L);
        assertThat(result.getCrawlingStatus()).isEqualTo("RUNNING");
    }

    @Test
    @DisplayName("관리자 대시보드 스냅샷을 한 번에 조회한다")
    void getDashboardSnapshot() {
        // given
        when(userRepository.count()).thenReturn(100L);
        when(subscriptionRepository.countByIsActiveTrue()).thenReturn(50L);
        when(notificationHistoryRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(20L);
        when(courseRepository.findMaxLastCrawledAt()).thenReturn(Optional.of(LocalDateTime.now()));
        when(courseCrawlerTargetService.getCurrentTarget()).thenReturn(
                AdminCrawlTargetResponse.builder()
                        .year("2026")
                        .semester("U211600020")
                        .build()
        );

        // when
        AdminDashboardSnapshotResponse result = adminService.getDashboardSnapshot();

        // then
        assertThat(result.getOverview().getTotalUsers()).isEqualTo(100L);
        assertThat(result.getOverview().getTotalActiveSubscriptions()).isEqualTo(50L);
        assertThat(result.getCrawlTarget().getYear()).isEqualTo("2026");
        assertThat(result.getCrawlTarget().getSemester()).isEqualTo("U211600020");
    }

    @Test
    @DisplayName("테스트 알림 전송이 성공한다")
    void sendTestNotification_Success() {
        // given
        String email = "test@example.com";
        User user = User.builder()
                .email(email)
                .emailEnabled(true)
                .build();

        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn(email);
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            // when
            adminService.sendTestNotification();

            // then
            verify(notificationService).sendTestNotification(eq(user), any());
        }
    }

    @Test
    @DisplayName("사용자 미존재 시 테스트 알림 전송이 실패한다")
    void sendTestNotification_UserNotFound() {
        // given
        String email = "notfound@example.com";

        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn(email);
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminService.sendTestNotification())
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
        }
    }
}
