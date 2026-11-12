package bhoon.sugang_helper.seatalert.notification.client;

import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.api.security.SensitiveDataRedactor;
import bhoon.sugang_helper.identity.api.notifications.WebPushSubscriptionManagement;
import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.dto.provider.NotificationTarget;
import bhoon.sugang_helper.seatalert.notification.exception.NotificationProviderException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
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
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class WebPushNotificationSender implements NotificationSender {

    private static final String ENDPOINT_FINGERPRINT_KEY = "endpointFingerprint";

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
        try {
            sendPush(target, title, message, idempotencyKey, "/", false);
        } catch (NotificationProviderException exception) {
            if (exception.getErrorCode() == ErrorCode.WEB_PUSH_INVALID_SUBSCRIPTION) {
                userDeviceWebPushManagement.deleteDeviceByToken(target.getRecipient());
            }
            throw exception;
        }
    }

    @Override
    public void sendDelivery(NotificationTarget target, String title, String message, String idempotencyKey,
                             String path) {
        sendPush(target, title, message, idempotencyKey, path, true);
    }

    private void sendPush(NotificationTarget target, String title, String message, String idempotencyKey,
                          String path, boolean durable) {
        if (pushService == null) {
            throw new CustomException(ErrorCode.WEB_PUSH_NOT_INITIALIZED);
        }

        if (target.getP256dh() == null || target.getAuth() == null) {
            throw new CustomException(ErrorCode.WEB_PUSH_MISSING_KEYS);
        }

        try {
            userDeviceWebPushManagement.validateEndpoint(target.getRecipient());
        } catch (CustomException exception) {
            if (!durable) {
                throw exception;
            }
            throw new NotificationProviderException(ErrorCode.WEB_PUSH_INVALID_SUBSCRIPTION,
                    false, "INVALID_RECIPIENT", null, exception);
        }
        String endpointFingerprint = SensitiveDataRedactor.fingerprint(target.getRecipient());

        try {
            Notification notification = createNotification(target, title, message, idempotencyKey, path, durable);
            logSendStarted(endpointFingerprint);
            @SuppressWarnings("deprecation")
            Future<HttpResponse> request = pushService.sendAsync(notification, Encoding.AESGCM);
            validateResponseStatus(awaitStatusCode(request));
            logSendSucceeded(endpointFingerprint);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            logSendFailed(endpointFingerprint, ErrorCode.WEB_PUSH_SEND_ERROR.getCode(), SensitiveDataRedactor.exceptionType(e));
            throw new NotificationProviderException(ErrorCode.WEB_PUSH_SEND_ERROR,
                    true, "ERROR", null, e);
        }
    }

    private Notification createNotification(NotificationTarget target, String title, String message,
                                            String idempotencyKey, String path, boolean durable)
            throws JsonProcessingException, GeneralSecurityException {
        String payload = objectMapper.writeValueAsString(new WebPushPayload(title, message, path, idempotencyKey));
        Notification.NotificationBuilder builder = Notification.builder().endpoint(target.getRecipient()).payload(payload);
        try {
            builder.userPublicKey(target.getP256dh()).userAuth(target.getAuth());
        } catch (IllegalArgumentException | InvalidKeySpecException exception) {
            if (!durable) {
                throw exception;
            }
            throw new NotificationProviderException(ErrorCode.WEB_PUSH_INVALID_SUBSCRIPTION,
                    false, "INVALID_RECIPIENT", null, exception);
        }
        if (StringUtils.hasText(idempotencyKey)) {
            builder.topic(idempotencyKey);
        }
        return builder.build();
    }

    private void validateResponseStatus(int statusCode) {
        if (statusCode == 404 || statusCode == 410) {
            throw new NotificationProviderException(ErrorCode.WEB_PUSH_INVALID_SUBSCRIPTION,
                    false, "INVALID_RECIPIENT", statusCode, null);
        }
        if (statusCode >= 400) {
            boolean retryable = statusCode == 429 || statusCode >= 500;
            throw new NotificationProviderException(ErrorCode.WEB_PUSH_SEND_ERROR,
                    retryable, failureReason(statusCode), statusCode, null);
        }
    }

    private String failureReason(int statusCode) {
        if (statusCode == 429) {
            return "RATE_LIMIT";
        }
        if (statusCode >= 500) {
            return "OUTAGE";
        }
        return "PERMANENT";
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
                .addKeyValue(ENDPOINT_FINGERPRINT_KEY, endpointFingerprint)
                .log("[WebPush] 단말 발송 시작.");
    }

    private void logSendSucceeded(String endpointFingerprint) {
        log.atDebug().addKeyValue(EVENT_KEY, "web_push_sent")
                .addKeyValue(ENDPOINT_FINGERPRINT_KEY, endpointFingerprint)
                .log("[WebPush] 단말 발송 완료.");
    }

    private void logSendFailed(String endpointFingerprint, String failureCode, String exceptionType) {
        log.atDebug().addKeyValue(EVENT_KEY, "web_push_send_failed")
                .addKeyValue(ENDPOINT_FINGERPRINT_KEY, endpointFingerprint)
                .addKeyValue("failureCode", failureCode)
                .addKeyValue("exceptionType", exceptionType)
                .log("[WebPush] 단말 발송 실패.");
    }

    private record WebPushPayload(String title, String body, String url, String idempotencyKey) {
    }
}
