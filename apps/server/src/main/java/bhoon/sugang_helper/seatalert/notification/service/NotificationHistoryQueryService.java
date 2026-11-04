package bhoon.sugang_helper.seatalert.notification.service;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.identity.user.service.UserIdentityQueryService;
import bhoon.sugang_helper.seatalert.notification.dto.result.NotificationHistoryItem;
import bhoon.sugang_helper.seatalert.notification.entity.NotificationHistory;
import bhoon.sugang_helper.seatalert.notification.repository.NotificationHistoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationHistoryQueryService {

    private final NotificationHistoryRepository repository;
    private final UserIdentityQueryService userIdentityQuery;

    public long countCreatedAfter(LocalDateTime start) {
        return repository.countByCreatedAtAfter(start);
    }

    public List<LocalDateTime> findCreatedAtAfter(LocalDateTime start) {
        return repository.findCreatedAtByCreatedAtAfter(start);
    }

    public List<NotificationHistoryItem> findLatest() {
        return repository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(NotificationHistoryQueryService::toItem)
                .toList();
    }

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
        String email = SecurityUtil.getCurrentUserEmail();
        Long userId = userIdentityQuery.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED))
                .id();
        return findForUser(userId, pageable);
    }

    private static NotificationHistoryItem toItem(NotificationHistory history) {
        return new NotificationHistoryItem(history.getId(), history.getUserId(), history.getCourseKey(),
                history.getTitle(), history.getMessage(), history.getChannel(), history.getCreatedAt());
    }
}
