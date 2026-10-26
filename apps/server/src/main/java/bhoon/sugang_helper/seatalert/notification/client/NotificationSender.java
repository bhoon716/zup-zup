package bhoon.sugang_helper.seatalert.notification.client;

import bhoon.sugang_helper.seatalert.enums.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.dto.provider.NotificationTarget;

public interface NotificationSender {

    boolean supports(NotificationChannel channel);

    void send(NotificationTarget target, String title, String message);




    default void send(NotificationTarget target, String title, String message, String idempotencyKey) {
        send(target, title, message);
    }
}
