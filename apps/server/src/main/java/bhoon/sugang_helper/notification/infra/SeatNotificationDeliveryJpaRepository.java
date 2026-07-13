package bhoon.sugang_helper.notification.infra;

import bhoon.sugang_helper.notification.domain.SeatNotificationDelivery;
import bhoon.sugang_helper.notification.domain.SeatNotificationDeliveryStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatNotificationDeliveryJpaRepository extends JpaRepository<SeatNotificationDelivery, Long> {

    boolean existsByOutboxIdAndUserIdAndChannel(Long outboxId, Long userId, NotificationChannel channel);

    long countByOutboxIdAndStatusNot(Long outboxId, SeatNotificationDeliveryStatus status);

    long countByOutboxIdAndStatus(Long outboxId, SeatNotificationDeliveryStatus status);

    long countByOutboxIdAndStatusIn(Long outboxId, List<SeatNotificationDeliveryStatus> statuses);

    Page<SeatNotificationDelivery> findByStatusOrderByDeadLetteredAtDescIdDesc(
            SeatNotificationDeliveryStatus status, Pageable pageable);

    long deleteByStatusAndDeadLetteredAtBefore(SeatNotificationDeliveryStatus status, LocalDateTime cutoff);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from SeatNotificationDelivery d where d.id = :id")
    Optional<SeatNotificationDelivery> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select d from SeatNotificationDelivery d
            join fetch d.outbox
            where d.id = :id
              and d.status = :status
              and d.claimToken = :claimToken
            """)
    Optional<SeatNotificationDelivery> findClaimedForDispatch(
            @Param("id") Long id,
            @Param("status") SeatNotificationDeliveryStatus status,
            @Param("claimToken") String claimToken);

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
