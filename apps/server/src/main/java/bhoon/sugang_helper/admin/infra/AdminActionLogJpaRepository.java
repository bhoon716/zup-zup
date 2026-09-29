package bhoon.sugang_helper.admin.infra;

import bhoon.sugang_helper.admin.domain.AdminActionLog;
import bhoon.sugang_helper.admin.domain.AdminActionLogRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminActionLogJpaRepository extends JpaRepository<AdminActionLog, Long>, AdminActionLogRepository {
}
