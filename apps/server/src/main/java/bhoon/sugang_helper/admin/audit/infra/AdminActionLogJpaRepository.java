package bhoon.sugang_helper.admin.audit.infra;

import bhoon.sugang_helper.admin.audit.domain.AdminActionLog;
import bhoon.sugang_helper.admin.audit.domain.AdminActionLogRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminActionLogJpaRepository extends JpaRepository<AdminActionLog, Long>, AdminActionLogRepository {
}
