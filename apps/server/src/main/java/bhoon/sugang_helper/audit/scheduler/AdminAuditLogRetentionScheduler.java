package bhoon.sugang_helper.audit.scheduler;

import bhoon.sugang_helper.audit.repository.AdminActionLogRepository;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAuditLogRetentionScheduler {

    private final AdminActionLogRepository adminActionLogRepository;

    @Value("${app.admin-audit.retention-days:0}")
    private long retentionDays;

    @PostConstruct
    void validateRetentionDays() {
        if (retentionDays < 0 || retentionDays > 36_500) {
            throw new IllegalArgumentException("관리자 감사 기록 보존 기간은 0~36500일이어야 합니다.");
        }
    }

    @Scheduled(cron = "${app.admin-audit.retention-cron:0 15 3 * * *}")
    @Transactional
    public void removeExpiredLogs() {
        validateRetentionDays();
        if (retentionDays == 0) {
            logRetentionDisabled();
            return;
        }

        long deletedCount = adminActionLogRepository.deleteByCreatedAtBefore(
                LocalDateTime.now().minusDays(retentionDays));
        logRetentionCompleted(deletedCount);
    }

    private void logRetentionDisabled() {
        log.atDebug().addKeyValue("event", "admin_audit_retention_skipped").addKeyValue("retentionDays", retentionDays)
                .log("[Admin Audit] 보존 기간 정리 비활성화로 실행 생략.");
    }

    private void logRetentionCompleted(long deletedCount) {
        log.atInfo().addKeyValue("event", "admin_audit_retention_completed").addKeyValue("deletedCount", deletedCount)
                .addKeyValue("retentionDays", retentionDays)
                .log("[Admin Audit] 보존 기간이 지난 기록 정리 완료. deletedCount={}", deletedCount);
    }

}
