package bhoon.sugang_helper.notification.infra;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.security.util.SensitiveDataRedactor;
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
        String tokenFingerprint = SensitiveDataRedactor.fingerprint(target.getRecipient());
        try {
            Message fcmMessage = Message.builder()
                    .setToken(target.getRecipient())
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(message)
                            .build())
                    .build();

            FirebaseMessaging.getInstance().send(fcmMessage);
            log.info("[FCM] Message sent. tokenFingerprint={}", tokenFingerprint);
        } catch (Exception e) {
            log.error("[FCM] Dispatch failed. tokenFingerprint={}, failureCode={}, exceptionType={}",
                    tokenFingerprint, ErrorCode.FCM_SEND_ERROR.getCode(), SensitiveDataRedactor.exceptionType(e));
            throw new CustomException(ErrorCode.FCM_SEND_ERROR);
        }
    }
}
