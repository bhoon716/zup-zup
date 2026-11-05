package bhoon.sugang_helper.seatalert.api;

import java.util.List;

public interface NotificationOperations {

    void sendAdminTestNotification(Long userId, List<NotificationChannel> channels);
}
