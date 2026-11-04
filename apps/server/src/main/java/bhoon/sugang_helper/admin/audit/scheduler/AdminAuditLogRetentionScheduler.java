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
            log.debug("[Admin Audit] 보존 기간 정리 비활성화로 실행 생략.");
            return;
        }

        long deletedCount = adminActionLogRepository.deleteByCreatedAtBefore(
                LocalDateTime.now().minusDays(retentionDays));
        log.info("[Admin Audit] 보존 기간이 지난 기록 정리 완료. deletedCount={}", deletedCount);
    }
}
