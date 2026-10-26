package bhoon.sugang_helper.admin.audit.scheduler;

import bhoon.sugang_helper.admin.audit.repository.AdminActionLogRepository;
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
