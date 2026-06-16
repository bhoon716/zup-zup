package bhoon.sugang_helper.feedback.infra;

import bhoon.sugang_helper.feedback.domain.AdminActionLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, Long> {
}
