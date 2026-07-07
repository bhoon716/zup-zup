package bhoon.sugang_helper.notification.domain;

import bhoon.sugang_helper.common.domain.RepositoryContract;
import java.time.LocalDateTime;
import java.util.List;

public interface NotificationHistoryRepository extends RepositoryContract<NotificationHistory, Long> {
    List<NotificationHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<NotificationHistory> findTop3ByUserIdOrderByCreatedAtDesc(Long userId);

    long countByCreatedAtAfter(LocalDateTime start);

    List<NotificationHistory> findTop50ByOrderByCreatedAtDesc();

    List<NotificationHistory> findByCreatedAtAfter(LocalDateTime start);

    List<LocalDateTime> findCreatedAtByCreatedAtAfter(LocalDateTime start);

    void deleteAllByUserId(Long userId);
}
