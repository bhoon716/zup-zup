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
import java.util.Map;

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
        // endpoint 파라미터에 JSON 형태의 subscription 정보가 들어올 것을 가정하거나,
        // 별도의 DTO를 거친 후 호출되도록 설계 필요 (여기선 단순화하여 recipient가 endpoint라고 가정)
        // 실제 구현 시 p256dh, auth 키가 필요하므로 NotificationSender 인터페이스 확장이 필요할 수 있음
        log.info("[WebPush] Attempting to send to: {}", endpoint);

        if (pushService == null) {
            log.warn("[WebPush] PushService is not initialized. Skipping.");
            return;
        }

        // Web Push는 복잡하므로 여기선 구조만 잡고, 실제 데이터는 JSON payload로 구성
        try {
            // 실제 구현에서는 UserDevice에서 정보를 가져와야 함
            // send(recipient, title, message) 인터페이스 한계상, payload에 모두 담긴 것으로 가정하거나
            // 별도의 발송 메소드 활용 권장
        } catch (Exception e) {
            log.error("[WebPush] Error sending notification: {}", e.getMessage());
        }
    }

    // Web Push를 위해 커스터마이징된 전송 메소드
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
