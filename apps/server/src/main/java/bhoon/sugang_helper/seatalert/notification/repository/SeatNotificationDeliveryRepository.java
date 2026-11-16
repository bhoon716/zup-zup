package bhoon.sugang_helper.seatalert.notification.repository;

import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationDelivery;
import bhoon.sugang_helper.seatalert.notification.enums.SeatNotificationDeliveryStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface SeatNotificationDeliveryRepository extends JpaRepository<SeatNotificationDelivery, Long> {

    @Query("""
            select d.outbox.id as outboxId, d.userId as userId, d.channel as channel, d.status as status
            from SeatNotificationDelivery d where d.outbox.id in :outboxIds and d.channel in :channels
            """)
    List<DeliveryRecipient> findExistingRecipientsByOutboxIds(
            @Param("outboxIds") List<Long> outboxIds, @Param("channels") List<NotificationChannel> channels);

    interface DeliveryRecipient {
        Long getOutboxId();

        Long getUserId();

        NotificationChannel getChannel();

        SeatNotificationDeliveryStatus getStatus();
    }

    @Query("select distinct d.status from SeatNotificationDelivery d where d.outbox.id = :outboxId")
    List<SeatNotificationDeliveryStatus> findStatusesByOutboxId(@Param("outboxId") Long outboxId);

    @EntityGraph(attributePaths = "outbox")
    Page<SeatNotificationDelivery> findByStatusOrderByDeadLetteredAtDescIdDesc(
            SeatNotificationDeliveryStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "outbox")
    Optional<SeatNotificationDelivery> findWithOutboxById(Long id);

    @Modifying
    @Query("delete from SeatNotificationDelivery d where d.status = :status and d.deadLetteredAt < :cutoff")
    int deleteByStatusAndDeadLetteredAtBefore(
            @Param("status") SeatNotificationDeliveryStatus status, @Param("cutoff") LocalDateTime cutoff);

    @Transactional(readOnly = true, timeout = 10)
    @QueryHints(@QueryHint(name = "jakarta.persistence.query.timeout", value = "5000"))
    @Query("""
            select d.channel as channel, count(d) as depth, min(d.nextAttemptAt) as oldestReadyAt
            from SeatNotificationDelivery d
            where (d.status = :pending and d.nextAttemptAt <= :now)
               or (d.status = :processing and d.lockedUntil < :now)
            group by d.channel
            """)
    List<ReadyBacklog> findReadyBacklog(
            @Param("pending") SeatNotificationDeliveryStatus pending,
            @Param("processing") SeatNotificationDeliveryStatus processing,
            @Param("now") LocalDateTime now);

    interface ReadyBacklog {

        NotificationChannel getChannel();

        long getDepth();

        LocalDateTime getOldestReadyAt();
    }

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
