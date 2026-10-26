package bhoon.sugang_helper.seatalert.notification.service;

import bhoon.sugang_helper.seatalert.notification.dto.result.NotificationHistoryItem;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface NotificationHistoryQuery {

    long countCreatedAfter(LocalDateTime start);

    List<LocalDateTime> findCreatedAtAfter(LocalDateTime start);

    List<NotificationHistoryItem> findLatest();

    List<NotificationHistoryItem> findLatestForUser(Long userId);

    Slice<NotificationHistoryItem> findForUser(Long userId, Pageable pageable);

    Slice<NotificationHistoryItem> findForCurrentUser(Pageable pageable);
}
