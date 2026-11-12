package bhoon.sugang_helper.seatalert.subscription.repository;

import bhoon.sugang_helper.seatalert.subscription.entity.Subscription;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByCourseKeyAndIsActiveTrue(String courseKey);

    Optional<Subscription> findByUserIdAndCourseKey(Long userId, String courseKey);

    long countByUserIdAndIsActiveTrue(Long userId);

    List<Subscription> findByUserId(Long userId);

    long countByCourseKeyStartingWithAndIsActiveTrue(String courseKeyPrefix);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Subscription s where s.id = :id")
    Optional<Subscription> findByIdForUpdate(@Param("id") Long id);

    @Query("select s.id from Subscription s where s.userId = :userId order by s.id")
    List<Long> findIdsByUserIdOrderById(@Param("userId") Long userId);

    @Query("""
            select distinct o.id as outboxId, s.userId as userId
            from SeatNotificationOutbox o
            join Subscription s on s.courseKey = o.courseKey
            where o.id in :outboxIds and s.isActive = true
              and (s.createdAt < o.createdAt or not exists (
                  select 1 from SubscriptionEnd e
                  where e.id.userId = s.userId and e.id.courseKey = s.courseKey
              ))
            """)
    List<OutboxRecipient> findActiveRecipientsByOutboxIds(@Param("outboxIds") List<Long> outboxIds);

    interface OutboxRecipient {
        Long getOutboxId();

        Long getUserId();
    }
}
