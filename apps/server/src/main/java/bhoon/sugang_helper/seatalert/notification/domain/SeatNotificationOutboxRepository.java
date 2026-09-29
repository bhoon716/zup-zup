package bhoon.sugang_helper.seatalert.notification.domain;

import bhoon.sugang_helper.common.domain.RepositoryContract;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface SeatNotificationOutboxRepository extends RepositoryContract<SeatNotificationOutbox, Long> {

    Optional<SeatNotificationOutbox> findByIdForUpdate(Long id);

    List<SeatNotificationOutbox> findByStatusForUpdate(SeatNotificationOutboxStatus status, Pageable pageable);
}
