package bhoon.sugang_helper.identity.api.notifications;

import java.util.List;
import java.util.Optional;

/** 다른 모듈이 사용하는 NotificationRecipients 계약. */
public interface NotificationRecipients {

    Optional<NotificationRecipient> findById(Long userId);

    Optional<NotificationRecipient> findByEmail(String email);

    List<NotificationRecipient> findByIds(List<Long> userIds);

    List<NotificationDevice> findDevicesByUserId(Long userId);

    List<NotificationDevice> findDevicesByUserIds(List<Long> userIds);
}
