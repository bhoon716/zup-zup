package bhoon.sugang_helper.identity.api.notifications;

import java.util.List;
import java.util.Optional;

public interface NotificationRecipients {

    Optional<NotificationRecipient> findById(Long userId);

    Optional<NotificationRecipient> findByEmail(String email);

    List<NotificationRecipient> findActiveByIdsForShare(List<Long> userIds);

    Optional<NotificationRecipient> findActiveByIdForUpdate(Long userId);

    Optional<Long> findLastActiveId();

    List<Long> findActiveIdsBetween(Long afterId, Long throughId, int limit);

    List<NotificationDevice> findDevicesByUserId(Long userId);

    List<NotificationDevice> findDevicesByUserIds(List<Long> userIds);
}
