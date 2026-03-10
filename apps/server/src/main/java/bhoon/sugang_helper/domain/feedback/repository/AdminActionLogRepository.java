package bhoon.sugang_helper.domain.feedback.repository;

import bhoon.sugang_helper.domain.feedback.entity.AdminActionLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, Long> {
}
