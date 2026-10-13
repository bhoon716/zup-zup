package bhoon.sugang_helper.admin.audit.repository;

import bhoon.sugang_helper.admin.audit.entity.AdminActionLog;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, Long> {

    Page<AdminActionLog> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    long deleteByCreatedAtBefore(LocalDateTime cutoff);
}
