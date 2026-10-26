package bhoon.sugang_helper.seatalert.notification.repository;

import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationOutbox;
import bhoon.sugang_helper.seatalert.notification.enums.SeatNotificationOutboxStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatNotificationOutboxRepository extends JpaRepository<SeatNotificationOutbox, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from SeatNotificationOutbox o where o.id = :id")
    Optional<SeatNotificationOutbox> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from SeatNotificationOutbox o where o.status = :status order by o.createdAt")
    List<SeatNotificationOutbox> findByStatusForUpdate(@Param("status") SeatNotificationOutboxStatus status,
                                                        Pageable pageable);
}
