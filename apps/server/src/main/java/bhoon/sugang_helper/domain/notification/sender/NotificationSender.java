package bhoon.sugang_helper.domain.notification.sender;

public interface NotificationSender {

    boolean supports(NotificationChannel channel);

    void send(NotificationTarget target, String title, String message);
}
