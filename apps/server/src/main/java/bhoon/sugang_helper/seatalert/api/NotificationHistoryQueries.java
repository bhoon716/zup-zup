package bhoon.sugang_helper.seatalert.api;

import java.util.List;

public interface NotificationHistoryQueries {

    List<NotificationHistoryItem> findLatestForUser(Long userId);
}
