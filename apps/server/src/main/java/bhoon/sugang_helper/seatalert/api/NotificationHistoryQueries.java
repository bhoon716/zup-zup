package bhoon.sugang_helper.seatalert.api;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationHistoryQueries {

    long countCreatedAfter(LocalDateTime start);

    List<LocalDateTime> findCreatedAtAfter(LocalDateTime start);

    List<NotificationHistoryItem> findLatest();

    List<NotificationHistoryItem> findLatestForUser(Long userId);
}
