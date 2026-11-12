package bhoon.sugang_helper.seatalert.notification.repository;

import bhoon.sugang_helper.seatalert.notification.entity.SeatInboxItem;
import bhoon.sugang_helper.seatalert.notification.enums.InboxNotificationType;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface SeatInboxRepository extends Repository<SeatInboxItem, Long> {

    List<SeatInboxItem> saveAll(Iterable<SeatInboxItem> items);

    SeatInboxItem save(SeatInboxItem item);

    boolean existsByUserIdAndTypeAndSourceKey(Long userId, InboxNotificationType type, String sourceKey);

    @Query("""
            select i from SeatInboxItem i
            where i.userId = :userId and i.deletedAt is null
              and (:type is null or i.type = :type)
              and (:read is null or (:read = true and i.readAt is not null) or (:read = false and i.readAt is null))
              and (:courseNamePattern is null or lower(i.courseName) like lower(:courseNamePattern) escape '!')
            order by i.occurredAt desc, i.id desc
            """)
    Page<SeatInboxItem> findVisible(@Param("userId") Long userId, @Param("read") Boolean read,
                                    @Param("type") InboxNotificationType type,
                                    @Param("courseNamePattern") String courseNamePattern, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from SeatInboxItem i where i.type = :type and i.sourceKey = :sourceKey order by i.id")
    List<SeatInboxItem> findBySourceForUpdate(@Param("type") InboxNotificationType type,
                                              @Param("sourceKey") String sourceKey);

    @Query("""
            select i.userId from SeatInboxItem i
            where i.type = :type and i.sourceKey = :sourceKey and i.userId in :userIds
            """)
    List<Long> findUsersBySource(@Param("type") InboxNotificationType type, @Param("sourceKey") String sourceKey,
                                 @Param("userIds") List<Long> userIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from SeatInboxItem i where i.type = :type and i.targetId = :targetId order by i.id")
    List<SeatInboxItem> findByTargetForUpdate(@Param("type") InboxNotificationType type,
                                              @Param("targetId") Long targetId);

    @Modifying
    @Query("update SeatInboxItem i set i.deletedAt = :now where i.id in :ids and i.deletedAt is null")
    void hide(@Param("ids") List<Long> ids, @Param("now") LocalDateTime now);

    @Modifying
    @Query("update SeatInboxItem i set i.available = :available where i.type = :type and i.targetId = :targetId")
    void setAvailable(@Param("type") InboxNotificationType type, @Param("targetId") Long targetId,
                      @Param("available") boolean available);

    @Query("""
            select i.outboxId as outboxId, i.userId as userId
            from SeatInboxItem i
            where i.outboxId in :outboxIds
            """)
    List<InboxRecipient> findExistingRecipients(@Param("outboxIds") List<Long> outboxIds);

    @Modifying
    @Query("""
            update SeatInboxItem i set i.readAt = :now
            where i.userId = :userId and i.id = :id and i.deletedAt is null and i.readAt is null
            """)
    void markRead(@Param("userId") Long userId, @Param("id") Long id, @Param("now") LocalDateTime now);

    @Modifying
    @Query("""
            update SeatInboxItem i set i.readAt = :now
            where i.userId = :userId and i.deletedAt is null and i.readAt is null
            """)
    void markAllRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("""
            update SeatInboxItem i set i.deletedAt = :now
            where i.userId = :userId and i.id = :id and i.deletedAt is null
            """)
    void softDelete(@Param("userId") Long userId, @Param("id") Long id, @Param("now") LocalDateTime now);

    @Modifying
    @Query("""
            update SeatInboxItem i set i.deletedAt = :now
            where i.userId = :userId and i.deletedAt is null
            """)
    void softDeleteAll(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    interface InboxRecipient {
        Long getOutboxId();

        Long getUserId();
    }
}
