package bhoon.sugang_helper.seatalert.notification.service;

import bhoon.sugang_helper.identity.api.notifications.NotificationRecipient;
import bhoon.sugang_helper.identity.api.notifications.NotificationRecipients;
import bhoon.sugang_helper.seatalert.api.NotificationPublications;
import bhoon.sugang_helper.seatalert.notification.entity.SeatInboxItem;
import bhoon.sugang_helper.seatalert.notification.enums.InboxNotificationType;
import bhoon.sugang_helper.seatalert.notification.repository.SeatInboxRepository;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class InboxPublicationService implements NotificationPublications {

    private static final int RECIPIENT_BATCH_SIZE = 250;

    private final NotificationRecipients recipients;
    private final SeatInboxRepository inbox;
    private final InboxChannelDeliveries deliveries;

    @Override
    public void publishFeedbackReply(Long userId, Long feedbackId, Long replyId) {
        List<NotificationRecipient> owners = recipients.findActiveByIdsForShare(List.of(userId));
        if (owners.isEmpty() || inbox.existsByUserIdAndTypeAndSourceKey(
                userId, InboxNotificationType.FEEDBACK_REPLY, replyId.toString())) {
            return;
        }
        publish(owners.getFirst(), SeatInboxItem.feedbackReply(userId, feedbackId, replyId, LocalDateTime.now()));
    }

    @Override
    public void hideFeedbackReply(Long replyId) {
        hideAndCancel(inbox.findBySourceForUpdate(InboxNotificationType.FEEDBACK_REPLY, replyId.toString()));
    }

    @Override
    public void hideFeedbackReplies(Long feedbackId) {
        hideAndCancel(inbox.findByTargetForUpdate(InboxNotificationType.FEEDBACK_REPLY, feedbackId));
    }

    @Override
    public void publishAnnouncement(Long announcementId, String title) {
        long lastId = recipients.findLastActiveId().orElse(0L);
        LocalDateTime now = LocalDateTime.now();
        for (long cursor = 0; cursor < lastId;) {
            List<Long> ids = recipients.findActiveIdsBetween(cursor, lastId, RECIPIENT_BATCH_SIZE);
            if (ids.isEmpty()) {
                return;
            }
            publishAnnouncementBatch(announcementId, title, now, ids);
            cursor = ids.getLast();
        }
    }

    @Override
    public void setAnnouncementAvailable(Long announcementId, boolean available) {
        inbox.setAvailable(InboxNotificationType.ANNOUNCEMENT, announcementId, available);
        if (!available) {
            deliveries.cancel(itemIds(inbox.findByTargetForUpdate(InboxNotificationType.ANNOUNCEMENT, announcementId)));
        }
    }

    private void publishAnnouncementBatch(Long announcementId, String title, LocalDateTime now, List<Long> ids) {
        List<NotificationRecipient> owners = recipients.findActiveByIdsForShare(ids);
        Set<Long> existing = new HashSet<>(inbox.findUsersBySource(
                InboxNotificationType.ANNOUNCEMENT, announcementId.toString(), ids));
        for (NotificationRecipient owner : owners) {
            if (!existing.contains(owner.id())) {
                publish(owner, SeatInboxItem.announcement(owner.id(), announcementId, title, now));
            }
        }
    }

    private void publish(NotificationRecipient owner, SeatInboxItem item) {
        SeatInboxItem stored = inbox.save(item);
        deliveries.enqueue(stored.getId(), owner, stored.getTitle(), stored.getMessage());
    }

    private void hideAndCancel(List<SeatInboxItem> items) {
        List<Long> ids = itemIds(items);
        if (!ids.isEmpty()) {
            inbox.hide(ids, LocalDateTime.now());
            deliveries.cancel(ids);
        }
    }

    private static List<Long> itemIds(List<SeatInboxItem> items) {
        return items.stream().map(SeatInboxItem::getId).toList();
    }
}
