package bhoon.sugang_helper.seatalert.subscription.repository;

import bhoon.sugang_helper.seatalert.subscription.entity.Subscription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByCourseKeyAndIsActiveTrue(String courseKey);

    Optional<Subscription> findByUserIdAndCourseKey(Long userId, String courseKey);

    long countByUserIdAndIsActiveTrue(Long userId);

    List<Subscription> findByUserId(Long userId);

    long countByIsActiveTrue();

    @Modifying
    @Query("delete from Subscription s where s.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Query("""
            select distinct o.id as outboxId, s.userId as userId
            from SeatNotificationOutbox o
            join Subscription s on s.courseKey = o.courseKey
            where o.id in :outboxIds and s.isActive = true
            """)
    List<OutboxRecipient> findActiveRecipientsByOutboxIds(@Param("outboxIds") List<Long> outboxIds);

    interface OutboxRecipient {
        Long getOutboxId();

        Long getUserId();
    }
}
