package bhoon.sugang_helper.notification.infra;

import bhoon.sugang_helper.notification.domain.SeatNotificationDelivery;
import bhoon.sugang_helper.notification.domain.SeatNotificationDeliveryStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatNotificationDeliveryJpaRepository extends JpaRepository<SeatNotificationDelivery, Long> {

    boolean existsByOutboxIdAndUserIdAndChannel(Long outboxId, Long userId, NotificationChannel channel);

    long countByOutboxIdAndStatusNot(Long outboxId, SeatNotificationDeliveryStatus status);

    long countByOutboxIdAndStatus(Long outboxId, SeatNotificationDeliveryStatus status);

    long countByOutboxIdAndStatusIn(Long outboxId, List<SeatNotificationDeliveryStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select d from SeatNotificationDelivery d
            where (d.status = :pending and d.nextAttemptAt <= :now)
               or (d.status = :processing and d.lockedUntil < :now)
            order by d.nextAttemptAt, d.id
            """)
    List<SeatNotificationDelivery> findReadyForUpdate(
            @Param("pending") SeatNotificationDeliveryStatus pending,
            @Param("processing") SeatNotificationDeliveryStatus processing,
            @Param("now") LocalDateTime now,
            Pageable pageable);
}
