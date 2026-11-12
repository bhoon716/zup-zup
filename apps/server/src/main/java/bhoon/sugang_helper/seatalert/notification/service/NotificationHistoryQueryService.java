package bhoon.sugang_helper.seatalert.notification.service;

import bhoon.sugang_helper.seatalert.api.NotificationHistoryQueries;
import bhoon.sugang_helper.identity.api.users.CurrentUserQuery;
import bhoon.sugang_helper.seatalert.api.NotificationHistoryItem;
import bhoon.sugang_helper.seatalert.notification.entity.NotificationHistory;
import bhoon.sugang_helper.seatalert.notification.repository.NotificationHistoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationHistoryQueryService implements NotificationHistoryQueries {

    private final NotificationHistoryRepository repository;
    private final CurrentUserQuery currentUserQuery;

    public List<NotificationHistoryItem> findLatestForUser(Long userId) {
        return repository.findTop3ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationHistoryQueryService::toItem)
                .toList();
    }

    public Slice<NotificationHistoryItem> findForUser(Long userId, Pageable pageable) {
        return repository.findByUserIdOrderByCreatedAtDescIdDesc(userId, pageable)
                .map(NotificationHistoryQueryService::toItem);
    }

    public Slice<NotificationHistoryItem> findForCurrentUser(Pageable pageable) {
        Long userId = currentUserQuery.requireCurrentUser().id();
        return findForUser(userId, pageable);
    }

    private static NotificationHistoryItem toItem(NotificationHistory history) {
        return new NotificationHistoryItem(history.getId(), history.getUserId(), history.getCourseKey(),
                history.getTitle(), history.getMessage(), history.getChannel(), history.getCreatedAt());
    }
}
