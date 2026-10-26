package bhoon.sugang_helper.seatalert.notification.repository;

import bhoon.sugang_helper.seatalert.notification.entity.NotificationHistory;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {



    Slice<NotificationHistory> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);




    List<NotificationHistory> findTop3ByUserIdOrderByCreatedAtDesc(Long userId);




    long countByCreatedAtAfter(LocalDateTime start);




    List<NotificationHistory> findTop50ByOrderByCreatedAtDesc();




    List<NotificationHistory> findByCreatedAtAfter(LocalDateTime start);




    @Query("SELECT n.createdAt FROM NotificationHistory n WHERE n.createdAt >= :start")
    List<LocalDateTime> findCreatedAtByCreatedAtAfter(@Param("start") LocalDateTime start);

    void deleteAllByUserId(Long userId);
}
