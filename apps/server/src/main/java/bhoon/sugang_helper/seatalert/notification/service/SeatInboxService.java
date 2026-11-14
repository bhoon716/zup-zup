package bhoon.sugang_helper.seatalert.notification.service;

import static bhoon.sugang_helper.seatalert.notification.entity.SeatInboxItem.UNAVAILABLE_MESSAGE;
import static bhoon.sugang_helper.seatalert.notification.entity.SeatInboxItem.UNAVAILABLE_TITLE;

import bhoon.sugang_helper.common.api.web.PageableGuard;
import bhoon.sugang_helper.identity.api.users.CurrentUserQuery;
import bhoon.sugang_helper.identity.events.UserWithdrawnEvent;
import bhoon.sugang_helper.seatalert.notification.dto.response.SeatInboxItemResponse;
import bhoon.sugang_helper.seatalert.notification.enums.InboxNotificationType;
import bhoon.sugang_helper.seatalert.notification.enums.SeatInboxReadFilter;
import bhoon.sugang_helper.seatalert.notification.enums.SeatInboxSort;
import bhoon.sugang_helper.seatalert.notification.enums.SeatInboxView;
import bhoon.sugang_helper.seatalert.notification.repository.SeatInboxRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SeatInboxService {

    private final SeatInboxRepository inboxRepository;
    private final CurrentUserQuery currentUserQuery;

    @Transactional(readOnly = true)
    public Page<SeatInboxItemResponse> getInbox(SeatInboxView view, SeatInboxReadFilter read, InboxNotificationType type,
                                               String courseName, String keyword, SeatInboxSort sort, int page) {
        Boolean readStatus = read == null ? null : read == SeatInboxReadFilter.READ;
        String keywordPattern = containsPattern(keyword == null ? null : keyword.strip());
        Sort.Direction direction = sort == SeatInboxSort.OLDEST ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageRequest = PageRequest.of(page, PageableGuard.PAGE_SIZE, Sort.by(direction, "occurredAt", "id"));
        return inboxRepository.findVisible(currentUserId(), readStatus, view.types(), type, containsPattern(courseName),
                keywordPattern, UNAVAILABLE_TITLE, UNAVAILABLE_MESSAGE, sort == SeatInboxSort.UNREAD_FIRST, pageRequest)
                .map(SeatInboxItemResponse::from);
    }

    @Transactional
    public void markRead(Long id) {
        inboxRepository.markRead(currentUserId(), id, LocalDateTime.now());
    }

    @Transactional
    public void markAllRead(SeatInboxView view) {
        inboxRepository.markAllRead(currentUserId(), view.types(), LocalDateTime.now());
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onUserWithdrawn(UserWithdrawnEvent event) {
        inboxRepository.softDeleteAll(event.userId(), LocalDateTime.now());
    }

    private Long currentUserId() {
        return currentUserQuery.requireCurrentUser().id();
    }

    private static String containsPattern(String text) {
        return text == null || text.isEmpty() ? null
                : "%" + text.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
}
