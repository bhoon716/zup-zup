package bhoon.sugang_helper.seatalert.notification.service;

import bhoon.sugang_helper.identity.api.users.CurrentUserQuery;
import bhoon.sugang_helper.identity.events.UserWithdrawnEvent;
import bhoon.sugang_helper.seatalert.notification.dto.response.SeatInboxItemResponse;
import bhoon.sugang_helper.seatalert.notification.enums.InboxNotificationType;
import bhoon.sugang_helper.seatalert.notification.enums.SeatInboxReadFilter;
import bhoon.sugang_helper.seatalert.notification.repository.SeatInboxRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SeatInboxService {

    private final SeatInboxRepository inboxRepository;
    private final CurrentUserQuery currentUserQuery;

    @Transactional(readOnly = true)
    public Page<SeatInboxItemResponse> getInbox(SeatInboxReadFilter read, InboxNotificationType type,
                                               String courseName, Pageable pageable) {
        Boolean readStatus = read == null ? null : read == SeatInboxReadFilter.READ;
        return inboxRepository.findVisible(currentUserId(), readStatus, type, containsPattern(courseName), pageable)
                .map(SeatInboxItemResponse::from);
    }

    @Transactional
    public void markRead(Long id) {
        inboxRepository.markRead(currentUserId(), id, LocalDateTime.now());
    }

    @Transactional
    public void markAllRead() {
        inboxRepository.markAllRead(currentUserId(), LocalDateTime.now());
    }

    @Transactional
    public void delete(Long id) {
        inboxRepository.softDelete(currentUserId(), id, LocalDateTime.now());
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onUserWithdrawn(UserWithdrawnEvent event) {
        inboxRepository.softDeleteAll(event.userId(), LocalDateTime.now());
    }

    private Long currentUserId() {
        return currentUserQuery.requireCurrentUser().id();
    }

    private static String containsPattern(String courseName) {
        return courseName == null || courseName.isEmpty() ? null
                : "%" + courseName.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
}
