package bhoon.sugang_helper.audit.repository;

import bhoon.sugang_helper.audit.entity.AdminActionLog;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, Long> {

    long deleteByCreatedAtBefore(LocalDateTime cutoff);
}
