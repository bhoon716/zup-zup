package bhoon.sugang_helper.feedback.infra;

import bhoon.sugang_helper.feedback.domain.AdminActionLog;
import bhoon.sugang_helper.feedback.domain.AdminActionLogRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminActionLogJpaRepository extends JpaRepository<AdminActionLog, Long>, AdminActionLogRepository {
}
