package bhoon.sugang_helper.notification.domain;

import bhoon.sugang_helper.common.domain.RepositoryContract;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface NotificationHistoryRepository extends RepositoryContract<NotificationHistory, Long> {
    Slice<NotificationHistory> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    List<NotificationHistory> findTop3ByUserIdOrderByCreatedAtDesc(Long userId);

    long countByCreatedAtAfter(LocalDateTime start);

    List<NotificationHistory> findTop50ByOrderByCreatedAtDesc();

    List<NotificationHistory> findByCreatedAtAfter(LocalDateTime start);

    List<LocalDateTime> findCreatedAtByCreatedAtAfter(LocalDateTime start);

    void deleteAllByUserId(Long userId);
}
