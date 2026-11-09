package bhoon.sugang_helper.audit.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.audit.repository.AdminActionLogRepository;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminAuditLogRetentionSchedulerTest {

    private static final String RETENTION_DAYS_FIELD = "retentionDays";

    @Mock
    private AdminActionLogRepository adminActionLogRepository;

    @Captor
    private ArgumentCaptor<LocalDateTime> cutoffCaptor;

    private AdminAuditLogRetentionScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new AdminAuditLogRetentionScheduler(adminActionLogRepository);
        ReflectionTestUtils.setField(scheduler, RETENTION_DAYS_FIELD, 180L);
    }

    @Test
    @DisplayName("180일이 지난 최소화된 관리자 감사 로그를 정리한다.")
    void removeExpiredLogs_deletesOnlyRecordsOlderThanRetentionCutoff() {
        // given
        LocalDateTime earliestExpectedCutoff = LocalDateTime.now().minusDays(180).minusSeconds(1);

        // when
        scheduler.removeExpiredLogs();

        // then
        LocalDateTime latestExpectedCutoff = LocalDateTime.now().minusDays(180).plusSeconds(1);
        verify(adminActionLogRepository).deleteByCreatedAtBefore(cutoffCaptor.capture());
        assertThat(cutoffCaptor.getValue()).isBetween(earliestExpectedCutoff, latestExpectedCutoff);
    }

    @Test
    @DisplayName("보존 기간을 0으로 설정하면 자동 정리를 수행하지 않는다.")
    void removeExpiredLogs_skipsCleanupWhenRetentionIsDisabled() {
        // given
        ReflectionTestUtils.setField(scheduler, RETENTION_DAYS_FIELD, 0L);

        // when
        scheduler.removeExpiredLogs();

        // then
        verify(adminActionLogRepository, never()).deleteByCreatedAtBefore(any());
    }
    @Test
    @DisplayName("설정하지 않은 감사 기록은 영구 보존한다.")
    void removeExpiredLogs_preservesRecordsByDefault() {
        // given
        scheduler = new AdminAuditLogRetentionScheduler(adminActionLogRepository);

        // when
        scheduler.removeExpiredLogs();

        // then
        verify(adminActionLogRepository, never()).deleteByCreatedAtBefore(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"application.yml", "application-prod.yml"})
    @DisplayName("실제 기본 및 운영 YAML에서도 환경변수가 없으면 자동 삭제하지 않는다")
    void configuredDefaultsPreserveAuditRecords(String resource) throws Exception {
        // given
        var properties = new YamlPropertySourceLoader().load("audit-retention", new FileSystemResource(Path.of("src/main/resources", resource)));
        String configured = String.valueOf(properties.getFirst().getProperty("app.admin-audit.retention-days"));

        // when
        String resolved = new MockEnvironment().resolveRequiredPlaceholders(configured);

        // then
        assertThat(resolved).isEqualTo("0");
    }

    @Test
    @DisplayName("음수 또는 과도한 보존 기간은 삭제 전에 거부한다.")
    void removeExpiredLogs_rejectsInvalidRetention() {
        // given
        ReflectionTestUtils.setField(scheduler, RETENTION_DAYS_FIELD, -1L);

        // when & then
        assertThatThrownBy(scheduler::removeExpiredLogs).isInstanceOf(IllegalArgumentException.class);
        ReflectionTestUtils.setField(scheduler, RETENTION_DAYS_FIELD, Long.MAX_VALUE);
        assertThatThrownBy(scheduler::removeExpiredLogs).isInstanceOf(IllegalArgumentException.class);
        verify(adminActionLogRepository, never()).deleteByCreatedAtBefore(any());
    }

}
