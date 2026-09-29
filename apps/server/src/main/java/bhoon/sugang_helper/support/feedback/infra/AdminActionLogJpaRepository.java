package bhoon.sugang_helper.support.feedback.infra;

import bhoon.sugang_helper.support.feedback.domain.AdminActionLog;
import bhoon.sugang_helper.support.feedback.domain.AdminActionLogRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminActionLogJpaRepository extends JpaRepository<AdminActionLog, Long>, AdminActionLogRepository {
}
