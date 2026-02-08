package bhoon.sugang_helper.domain.notification.sender;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.Security;

@Slf4j
@Component
public class WebPushNotificationSender implements NotificationSender {

    private final String publicKey;
    private final String privateKey;
    private final String subject;
    private final ObjectMapper objectMapper;
    private final bhoon.sugang_helper.domain.user.service.UserDeviceService userDeviceService;
    private PushService pushService;

    public WebPushNotificationSender(
            @Value("${app.webpush.public-key}") String publicKey,
            @Value("${app.webpush.private-key}") String privateKey,
            @Value("${app.webpush.subject}") String subject,
            ObjectMapper objectMapper,
            bhoon.sugang_helper.domain.user.service.UserDeviceService userDeviceService) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.subject = subject;
        this.objectMapper = objectMapper;
        this.userDeviceService = userDeviceService;
    }

    @PostConstruct
    public void init() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
            log.info("[WebPush] Registered BouncyCastleProvider");
        }
        try {
            if (publicKey != null && !publicKey.trim().isEmpty() &&
                    privateKey != null && !privateKey.trim().isEmpty()) {
                this.pushService = new PushService(publicKey, privateKey, subject);
                log.info("[WebPush] PushService initialized");
            }
        } catch (Exception e) {
            // 초기화 실패는 비즈니스 예외가 아니므로 런타임 예외로 던져서 기동 중단
            throw new RuntimeException("WebPush PushService initialization failed", e);
        }
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.WEB;
    }

    @Override
    public void send(NotificationTarget target, String title, String message) {
        if (pushService == null) {
            throw new CustomException(ErrorCode.WEBPUSH_NOT_INITIALIZED);
        }

        if (target.getP256dh() == null || target.getAuth() == null) {
            throw new CustomException(ErrorCode.WEBPUSH_MISSING_KEYS);
        }

        try {
            String payload = objectMapper.writeValueAsString(new WebPushPayload(title, message, "/"));

            Notification notification = new Notification(
                    target.getRecipient(),
                    target.getP256dh(),
                    target.getAuth(),
                    payload);

            log.info("[WebPush] Sending notification to {}", target.getRecipient());
            var response = pushService.send(notification);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 404 || statusCode == 410) {
                userDeviceService.deleteDeviceByToken(target.getRecipient());
                throw new CustomException(ErrorCode.WEBPUSH_INVALID_SUBSCRIPTION);
            }

            if (statusCode >= 400) {
                throw new CustomException(ErrorCode.WEBPUSH_SEND_ERROR, "Status: " + statusCode);
            }

            log.info("[WebPush] Sent successfully to {}", target.getRecipient());
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.WEBPUSH_SEND_ERROR, e.getMessage());
        }
    }

    private record WebPushPayload(String title, String body, String url) {
    }
}
