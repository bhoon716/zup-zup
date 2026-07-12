package bhoon.sugang_helper.admin.application;

import bhoon.sugang_helper.feedback.domain.AdminActionLogRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 최소화된 관리자 감사 로그의 운영상 보존 기간을 적용합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAuditLogRetentionScheduler {

    private final AdminActionLogRepository adminActionLogRepository;

    @Value("${app.admin-audit.retention-days:180}")
    private long retentionDays;

    @Scheduled(cron = "${app.admin-audit.retention-cron:0 15 3 * * *}")
    @Transactional
    public void removeExpiredLogs() {
        if (retentionDays <= 0) {
            log.info("[Admin Audit] Retention cleanup skipped because it is disabled.");
            return;
        }

        long deletedCount = adminActionLogRepository.deleteByCreatedAtBefore(
                LocalDateTime.now().minusDays(retentionDays));
        log.info("[Admin Audit] Retention cleanup completed. deletedCount={}", deletedCount);
    }
}
