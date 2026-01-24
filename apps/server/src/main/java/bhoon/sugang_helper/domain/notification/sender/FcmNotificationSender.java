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
    public void send(NotificationTarget target, String title, String message) {
        try {
            Message fcmMessage = Message.builder()
                    .setToken(target.getRecipient())
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(message)
                            .build())
                    .build();

            String response = FirebaseMessaging.getInstance().send(fcmMessage);
            log.info("[FCM] Sent message to: {}, response: {}", target.getRecipient(), response);
        } catch (Exception e) {
            log.error("[FCM] 발송 실패 - 토큰: {}, 에러: {}", target.getRecipient(), e.getMessage(), e);
            throw new CustomException(ErrorCode.FCM_SEND_ERROR, e.getMessage());
        }
    }
}
