package bhoon.sugang_helper.notification.infra;

public interface NotificationSender {

    boolean supports(NotificationChannel channel);

    void send(NotificationTarget target, String title, String message);

    /**
     * Durable delivery는 provider가 수용할 수 있는 짧은 idempotency key를 함께 전달합니다.
     */
    default void send(NotificationTarget target, String title, String message, String idempotencyKey) {
        send(target, title, message);
    }
}
