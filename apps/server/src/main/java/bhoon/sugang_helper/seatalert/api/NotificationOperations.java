package bhoon.sugang_helper.seatalert.api;

import java.util.List;

/** 다른 모듈이 사용하는 NotificationOperations 계약. */
public interface NotificationOperations {

    void sendAdminTestNotification(Long userId, List<NotificationChannel> channels);
}
