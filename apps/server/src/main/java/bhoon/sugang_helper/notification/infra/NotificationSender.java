package bhoon.sugang_helper.notification.infra;

public interface NotificationSender {

    boolean supports(NotificationChannel channel);

    void send(NotificationTarget target, String title, String message);
}
