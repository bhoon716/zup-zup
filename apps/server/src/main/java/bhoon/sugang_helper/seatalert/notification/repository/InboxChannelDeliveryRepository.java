package bhoon.sugang_helper.seatalert.notification.repository;

import bhoon.sugang_helper.seatalert.notification.entity.InboxChannelDelivery;
import bhoon.sugang_helper.seatalert.notification.entity.InboxChannelDelivery.Status;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InboxChannelDeliveryRepository extends JpaRepository<InboxChannelDelivery, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select d from InboxChannelDelivery d
            where (d.status = :pending and d.nextAttemptAt <= :now)
               or (d.status = :processing and d.lockedUntil < :now)
            order by d.nextAttemptAt, d.id
            """)
    List<InboxChannelDelivery> findReadyForUpdate(@Param("pending") Status pending,
                                                  @Param("processing") Status processing,
                                                  @Param("now") LocalDateTime now,
                                                  Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from InboxChannelDelivery d where d.id = :id")
    Optional<InboxChannelDelivery> findByIdForUpdate(@Param("id") String id);

    @Modifying(flushAutomatically = true)
    @Query("""
            update InboxChannelDelivery d
               set d.status = :skipped, d.finishedAt = :now, d.claimToken = null, d.lockedUntil = null
             where d.inboxId in :inboxIds and d.status in :activeStatuses
            """)
    int cancel(@Param("inboxIds") List<Long> inboxIds, @Param("activeStatuses") List<Status> activeStatuses,
               @Param("skipped") Status skipped, @Param("now") LocalDateTime now);

    @Modifying
    @Query("delete from InboxChannelDelivery d where d.finishedAt < :cutoff")
    int deleteFinishedBefore(@Param("cutoff") LocalDateTime cutoff);
}
