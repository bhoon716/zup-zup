package bhoon.sugang_helper.seatalert.api;

import java.time.LocalDateTime;
import java.util.List;

/** 다른 모듈이 사용하는 NotificationHistoryQueries 계약. */
public interface NotificationHistoryQueries {

    long countCreatedAfter(LocalDateTime start);

    List<LocalDateTime> findCreatedAtAfter(LocalDateTime start);

    List<NotificationHistoryItem> findLatest();

    List<NotificationHistoryItem> findLatestForUser(Long userId);
}
