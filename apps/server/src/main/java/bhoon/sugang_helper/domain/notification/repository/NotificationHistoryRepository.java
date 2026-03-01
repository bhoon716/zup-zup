package bhoon.sugang_helper.domain.notification.repository;

import bhoon.sugang_helper.domain.notification.entity.NotificationHistory;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {
    /**
     * 특정 사용자의 알림 수신 내역을 최신순으로 조회합니다.
     */
    List<NotificationHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 특정 시점 이후에 발생한 알림의 총 개수를 집계합니다.
     */
    long countByCreatedAtAfter(LocalDateTime start);

    /**
     * 전체 시스템에서 최근 발생한 알림 내역 50건을 조회합니다.
     */
    List<NotificationHistory> findTop50ByOrderByCreatedAtDesc();

    /**
     * 특정 시점 이후에 발생한 전체 알림 목록을 조회합니다.
     */
    List<NotificationHistory> findByCreatedAtAfter(LocalDateTime start);
}
