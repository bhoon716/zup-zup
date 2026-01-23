package bhoon.sugang_helper.domain.notification.sender;

public interface NotificationSender {
    /**
     * Checks if this sender supports the given channel.
     */
    boolean supports(NotificationChannel channel);

    /**
     * Sends a notification.
     * 
     * @param recipient Target identifier (email, token, etc.)
     * @param title     Notification title
     * @param message   Notification body
     */
    void send(String recipient, String title, String message);
}
