package bhoon.sugang_helper.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.feedback.domain.AdminActionLogRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminAuditLogRetentionSchedulerTest {

    @Mock
    private AdminActionLogRepository adminActionLogRepository;
    @Captor
    private ArgumentCaptor<LocalDateTime> cutoffCaptor;

    private AdminAuditLogRetentionScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new AdminAuditLogRetentionScheduler(adminActionLogRepository);
        ReflectionTestUtils.setField(scheduler, "retentionDays", 180L);
    }

    @Test
    @DisplayName("180일이 지난 최소화된 관리자 감사 로그를 정리한다.")
    void removeExpiredLogs_deletesOnlyRecordsOlderThanRetentionCutoff() {
        LocalDateTime earliestExpectedCutoff = LocalDateTime.now().minusDays(180).minusSeconds(1);

        scheduler.removeExpiredLogs();

        LocalDateTime latestExpectedCutoff = LocalDateTime.now().minusDays(180).plusSeconds(1);
        verify(adminActionLogRepository).deleteByCreatedAtBefore(cutoffCaptor.capture());
        assertThat(cutoffCaptor.getValue()).isBetween(earliestExpectedCutoff, latestExpectedCutoff);
    }

    @Test
    @DisplayName("보존 기간을 0 이하로 설정하면 자동 정리를 수행하지 않는다.")
    void removeExpiredLogs_skipsCleanupWhenRetentionIsDisabled() {
        ReflectionTestUtils.setField(scheduler, "retentionDays", 0L);

        scheduler.removeExpiredLogs();

        verify(adminActionLogRepository, never()).deleteByCreatedAtBefore(any());
    }
}
