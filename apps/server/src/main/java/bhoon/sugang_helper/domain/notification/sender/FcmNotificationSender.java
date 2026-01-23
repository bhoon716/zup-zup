package bhoon.sugang_helper.domain.notification.sender;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FcmNotificationSender implements NotificationSender {

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.FCM;
    }

    @Override
    public void send(String recipient, String title, String message) {
        try {
            Message fcmMessage = Message.builder()
                    .setToken(recipient)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(message)
                            .build())
                    .build();

            String response = FirebaseMessaging.getInstance().send(fcmMessage);
            log.info("[FCM] Sent message to: {}, response: {}", recipient, response);
        } catch (Exception e) {
            log.error("[FCM] Failed to send message to: {}, error: {}", recipient, e.getMessage());
            // FCM 토큰 만료 등의 이슈가 있을 수 있으나, 일단 로그만 남기고 예외를 던지지는 않음 (멀티 전송 고려)
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "FCM send error: " + e.getMessage());
        }
    }
}
