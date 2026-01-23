package bhoon.sugang_helper.domain.notification.sender;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
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
    private PushService pushService;

    public WebPushNotificationSender(
            @Value("${app.webpush.public-key:}") String publicKey,
            @Value("${app.webpush.private-key:}") String privateKey,
            @Value("${app.webpush.subject:mailto:admin@example.com}") String subject) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.subject = subject;
    }

    @PostConstruct
    public void init() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        try {
            if (!publicKey.isEmpty() && !privateKey.isEmpty()) {
                this.pushService = new PushService(publicKey, privateKey, subject);
            }
        } catch (Exception e) {
            log.warn("[WebPush] Initialization failed: {}", e.getMessage());
        }
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.WEB;
    }

    @Override
    public void send(String endpoint, String title, String message) {
        log.info("[WebPush] Attempting to send to: {}", endpoint);
        if (pushService == null)
            return;
    }

    public void sendWebPush(String endpoint, String p256dh, String auth, String title, String message) {
        if (pushService == null)
            return;

        try {
            Notification notification = new Notification(
                    endpoint,
                    p256dh,
                    auth,
                    String.format("{\"title\":\"%s\", \"body\":\"%s\"}", title, message));
            pushService.send(notification);
            log.info("[WebPush] Sent successfully to: {}", endpoint);
        } catch (Exception e) {
            log.error("[WebPush] Failed to send to: {}, error: {}", endpoint, e.getMessage());
        }
    }
}
