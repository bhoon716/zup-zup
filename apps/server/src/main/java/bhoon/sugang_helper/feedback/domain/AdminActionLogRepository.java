package bhoon.sugang_helper.feedback.domain;

import bhoon.sugang_helper.common.domain.RepositoryContract;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminActionLogRepository extends RepositoryContract<AdminActionLog, Long> {

    Page<AdminActionLog> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    long deleteByCreatedAtBefore(LocalDateTime cutoff);
}
