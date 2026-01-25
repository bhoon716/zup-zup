package bhoon.sugang_helper.domain.notification.repository;

import bhoon.sugang_helper.domain.notification.entity.NotificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {
    List<NotificationHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByCreatedAtAfter(LocalDateTime start);
}
