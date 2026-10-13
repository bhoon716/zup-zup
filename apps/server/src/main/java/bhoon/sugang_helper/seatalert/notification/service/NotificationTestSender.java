package bhoon.sugang_helper.seatalert.notification.service;

import bhoon.sugang_helper.seatalert.enums.NotificationChannel;
import java.util.List;

public interface NotificationTestSender {

    void sendAdminTestNotification(Long userId, List<NotificationChannel> channels);
}
