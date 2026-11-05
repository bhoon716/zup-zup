package bhoon.sugang_helper.identity.api.notifications;

import java.util.List;
import java.util.Optional;

public interface NotificationRecipients {

    Optional<NotificationRecipient> findById(Long userId);

    Optional<NotificationRecipient> findByEmail(String email);

    List<NotificationRecipient> findByIds(List<Long> userIds);

    List<NotificationDevice> findDevicesByUserId(Long userId);

    List<NotificationDevice> findDevicesByUserIds(List<Long> userIds);
}
