package bhoon.sugang_helper.domain.notification.repository;

import bhoon.sugang_helper.domain.notification.entity.NotificationHistory;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {
    List<NotificationHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByCreatedAtAfter(LocalDateTime start);
}
