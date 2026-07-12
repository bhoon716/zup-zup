package bhoon.sugang_helper.notification.infra;

import bhoon.sugang_helper.notification.domain.SeatNotificationOutbox;
import bhoon.sugang_helper.notification.domain.SeatNotificationOutboxRepository;
import bhoon.sugang_helper.notification.domain.SeatNotificationOutboxStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatNotificationOutboxJpaRepository extends JpaRepository<SeatNotificationOutbox, Long>,
        SeatNotificationOutboxRepository {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from SeatNotificationOutbox o where o.status = :status order by o.createdAt")
    List<SeatNotificationOutbox> findByStatusForUpdate(@Param("status") SeatNotificationOutboxStatus status,
                                                        Pageable pageable);
}
