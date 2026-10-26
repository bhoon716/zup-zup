package bhoon.sugang_helper.identity.user.service;

import bhoon.sugang_helper.identity.user.dto.result.NotificationDevice;
import bhoon.sugang_helper.identity.user.dto.result.NotificationRecipient;
import java.util.List;
import java.util.Optional;

public interface UserNotificationQuery {

    Optional<NotificationRecipient> findById(Long userId);

    Optional<NotificationRecipient> findByEmail(String email);

    List<NotificationRecipient> findByIds(List<Long> userIds);

    List<NotificationDevice> findDevicesByUserId(Long userId);

    List<NotificationDevice> findDevicesByUserIds(List<Long> userIds);
}
