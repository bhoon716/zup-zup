package bhoon.sugang_helper.seatalert.notification.contract;

import bhoon.sugang_helper.seatalert.contract.NotificationChannel;
import java.util.List;

public interface NotificationTestSender {

    void sendAdminTestNotification(Long userId, List<NotificationChannel> channels);

    void sendUserTestNotification(Long userId, List<NotificationChannel> channels);
}
