package bhoon.sugang_helper.seatalert.notification.client;

import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.api.security.SensitiveDataRedactor;
import bhoon.sugang_helper.identity.api.notifications.WebPushSubscriptionManagement;
import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.dto.provider.NotificationTarget;
import bhoon.sugang_helper.seatalert.notification.exception.NotificationProviderException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.security.Security;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WebPushNotificationSender implements NotificationSender {

    private static final String EVENT_KEY = "event";

    private final String publicKey;
    private final String privateKey;
    private final String subject;
    private final ObjectMapper objectMapper;
    private final WebPushSubscriptionManagement userDeviceWebPushManagement;
    private PushService pushService;

    public WebPushNotificationSender(
            @Value("${app.webpush.public-key}") String publicKey,
            @Value("${app.webpush.private-key}") String privateKey,
            @Value("${app.webpush.subject}") String subject,
            ObjectMapper objectMapper,
            WebPushSubscriptionManagement userDeviceWebPushManagement) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.subject = subject;
        this.objectMapper = objectMapper;
        this.userDeviceWebPushManagement = userDeviceWebPushManagement;
    }

    @PostConstruct
    public void init() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
            logCryptoProviderRegistered();
        }
        try {
            if (publicKey != null && !publicKey.trim().isEmpty() &&
                    privateKey != null && !privateKey.trim().isEmpty()) {
                this.pushService = new PushService(publicKey, privateKey, subject);
                logInitialized();
            }
        } catch (Exception e) {
            throw new RuntimeException("웹푸시 PushService 초기화에 실패했습니다.", e);
        }
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.WEB;
    }

    @Override
    public void send(NotificationTarget target, String title, String message) {
        send(target, title, message, null);
    }

    @Override
    public void send(NotificationTarget target, String title, String message, String idempotencyKey) {
        if (pushService == null) {
            throw new CustomException(ErrorCode.WEB_PUSH_NOT_INITIALIZED);
        }

        if (target.getP256dh() == null || target.getAuth() == null) {
            throw new CustomException(ErrorCode.WEB_PUSH_MISSING_KEYS);
        }

        userDeviceWebPushManagement.validateEndpoint(target.getRecipient());
        String endpointFingerprint = SensitiveDataRedactor.fingerprint(target.getRecipient());

        try {
            String payload = objectMapper.writeValueAsString(new WebPushPayload(title, message, "/", idempotencyKey));
            Notification.NotificationBuilder notificationBuilder = Notification.builder()
                    .endpoint(target.getRecipient())
                    .userPublicKey(target.getP256dh())
                    .userAuth(target.getAuth())
                    .payload(payload);
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                notificationBuilder.topic(idempotencyKey);
            }
            Notification notification = notificationBuilder.build();

            logSendStarted(endpointFingerprint);
            @SuppressWarnings("deprecation")
            Future<HttpResponse> request = pushService.sendAsync(notification, Encoding.AESGCM);
            int statusCode = awaitStatusCode(request);

            if (statusCode == 404 || statusCode == 410) {
                userDeviceWebPushManagement.deleteDeviceByToken(target.getRecipient());
                throw new NotificationProviderException(ErrorCode.WEB_PUSH_INVALID_SUBSCRIPTION,
                        false, "INVALID_RECIPIENT", statusCode, null);
            }

            if (statusCode >= 400) {
                boolean retryable = statusCode == 429 || statusCode >= 500;
                String reason = statusCode == 429 ? "RATE_LIMIT" : statusCode >= 500 ? "OUTAGE" : "PERMANENT";
                throw new NotificationProviderException(ErrorCode.WEB_PUSH_SEND_ERROR,
                        retryable, reason, statusCode, null);
            }

            logSendSucceeded(endpointFingerprint);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            logSendFailed(endpointFingerprint, ErrorCode.WEB_PUSH_SEND_ERROR.getCode(), SensitiveDataRedactor.exceptionType(e));
            throw new NotificationProviderException(ErrorCode.WEB_PUSH_SEND_ERROR,
                    true, "ERROR", null, e);
        }
    }

    private int awaitStatusCode(Future<HttpResponse> request) throws ExecutionException, InterruptedException {
        try {
            return request.get().getStatusLine().getStatusCode();
        } catch (InterruptedException exception) {
            request.cancel(true);
            Thread.currentThread().interrupt();
            throw exception;
        }
    }

    private void logCryptoProviderRegistered() {
        log.atInfo().addKeyValue(EVENT_KEY, "web_push_crypto_provider_registered")
                .log("[WebPush] 암호화 제공자 등록 완료.");
    }

    private void logInitialized() {
        log.atInfo().addKeyValue(EVENT_KEY, "web_push_initialized")
                .log("[WebPush] 발송 서비스 초기화 완료.");
    }

    private void logSendStarted(String endpointFingerprint) {
        log.atDebug().addKeyValue(EVENT_KEY, "web_push_send_started")
                .addKeyValue("endpointFingerprint", endpointFingerprint)
                .log("[WebPush] 단말 발송 시작.");
    }

    private void logSendSucceeded(String endpointFingerprint) {
        log.atDebug().addKeyValue(EVENT_KEY, "web_push_sent")
                .addKeyValue("endpointFingerprint", endpointFingerprint)
                .log("[WebPush] 단말 발송 완료.");
    }

    private void logSendFailed(String endpointFingerprint, String failureCode, String exceptionType) {
        log.atDebug().addKeyValue(EVENT_KEY, "web_push_send_failed")
                .addKeyValue("endpointFingerprint", endpointFingerprint)
                .addKeyValue("failureCode", failureCode)
                .addKeyValue("exceptionType", exceptionType)
                .log("[WebPush] 단말 발송 실패.");
    }

    private record WebPushPayload(String title, String body, String url, String idempotencyKey) {
    }
}
