package bhoon.sugang_helper.notification.infra;

import bhoon.sugang_helper.notification.domain.NotificationHistory;
import bhoon.sugang_helper.notification.domain.NotificationHistoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationHistoryJpaRepository extends JpaRepository<NotificationHistory, Long>,
        NotificationHistoryRepository {
    /**
     * 특정 사용자의 알림 수신 내역을 최신순으로 조회합니다.
     */
    Slice<NotificationHistory> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    /**
     * 특정 사용자의 최신 알림 수신 내역 일부만 조회합니다.
     */
    List<NotificationHistory> findTop3ByUserIdOrderByCreatedAtDesc(Long userId);

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

    /**
     * 특정 시점 이후에 발생한 전체 알림의 생성 시각만 고속으로 조회합니다 (성능 최적화).
     */
    @Query("SELECT n.createdAt FROM NotificationHistory n WHERE n.createdAt >= :start")
    List<LocalDateTime> findCreatedAtByCreatedAtAfter(@Param("start") LocalDateTime start);
}
