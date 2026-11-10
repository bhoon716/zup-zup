package bhoon.sugang_helper.seatalert.notification.client;

import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.api.security.SensitiveDataRedactor;
import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.dto.provider.NotificationTarget;
import bhoon.sugang_helper.seatalert.notification.exception.NotificationProviderException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@Slf4j
public class DiscordNotificationSender implements NotificationSender {

    private static final String BOT_AUTHORIZATION_PREFIX = "Bot ";
    private static final String EVENT_KEY = "event";
    private static final String CHANNEL_ID_FIELD = "id";
    private static final String USER_ID_FINGERPRINT_KEY = "userIdFingerprint";

    private RestClient restClient;

    @Value("${app.discord.bot-token:}")
    private String botToken;

    public DiscordNotificationSender() {
        this(2_000, 5_000);
    }

    @Autowired
    public DiscordNotificationSender(
            @Value("${app.notification.provider.connect-timeout-ms:2000}") long connectTimeoutMs,
            @Value("${app.notification.provider.read-timeout-ms:5000}") long readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl("https://discord.com/api/v10")
                .build();
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.DISCORD;
    }

    @Override
    public void send(NotificationTarget target, String title, String message) {
        send(target, title, message, null);
    }

    @Override
    public void send(NotificationTarget target, String title, String message, String idempotencyKey) {
        if (!StringUtils.hasText(botToken) || botToken.equals("<DISCORD_BOT_TOKEN>")) {
            logMissingBotToken();
            return;
        }

        String userId = target.getRecipient();
        String userIdFingerprint = SensitiveDataRedactor.fingerprint(userId);
        try {
            String channelId = openDirectMessageChannel(userId);
            sendMessageToChannel(channelId, title, message, idempotencyKey);
            logMessageSent(userIdFingerprint);
        } catch (Exception e) {
            logMessageFailed(userIdFingerprint, ErrorCode.DISCORD_SEND_ERROR.getCode(), SensitiveDataRedactor.exceptionType(e));
            throw toProviderException(e);
        }
    }

    private String openDirectMessageChannel(String userId) {
        Map<String, Object> channelResponse = restClient.post()
                .uri("/users/@me/channels")
                .header(HttpHeaders.AUTHORIZATION, BOT_AUTHORIZATION_PREFIX + botToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("recipient_id", userId))
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        if (channelResponse == null || !channelResponse.containsKey(CHANNEL_ID_FIELD)) {
            throw new RuntimeException("디스코드 개인 메시지 채널 생성에 실패했습니다.");
        }
        return (String) channelResponse.get(CHANNEL_ID_FIELD);
    }

    private void sendMessageToChannel(String channelId, String title, String message, String idempotencyKey) {
        String content = String.format("**%s**\n\n%s", title, message);
        Map<String, Object> messagePayload = new HashMap<>();
        messagePayload.put("content", content);
        if (StringUtils.hasText(idempotencyKey)) {
            messagePayload.put("nonce", idempotencyKey);
            messagePayload.put("enforce_nonce", true);
        }
        restClient.post()
                .uri("/channels/{channelId}/messages", channelId)
                .header(HttpHeaders.AUTHORIZATION, BOT_AUTHORIZATION_PREFIX + botToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(messagePayload)
                .retrieve()
                .toEntity(String.class);
    }

    private NotificationProviderException toProviderException(Exception failure) {
        Integer statusCode = null;
        if (failure instanceof RestClientResponseException responseException) {
            statusCode = responseException.getStatusCode().value();
        }
        boolean retryable = statusCode == null || statusCode == 429 || statusCode >= 500;
        String reason = "ERROR";
        if (statusCode != null && statusCode == 429) {
            reason = "RATE_LIMIT";
        }
        if (statusCode != null && statusCode >= 500) {
            reason = "OUTAGE";
        }
        return new NotificationProviderException(ErrorCode.DISCORD_SEND_ERROR, retryable, reason, statusCode, failure);
    }

    private void logMissingBotToken() {
        log.atWarn().addKeyValue(EVENT_KEY, "discord_notification_skipped")
                .addKeyValue("reason", "BOT_TOKEN_MISSING")
                .log("[Discord] 봇 토큰 미설정으로 알림 발송 생략.");
    }

    private void logMessageSent(String userIdFingerprint) {
        log.atDebug().addKeyValue(EVENT_KEY, "discord_notification_sent")
                .addKeyValue(USER_ID_FINGERPRINT_KEY, userIdFingerprint)
                .log("[Discord] 개인 메시지 발송 완료.");
    }

    private void logMessageFailed(String userIdFingerprint, String failureCode, String exceptionType) {
        log.atDebug().addKeyValue(EVENT_KEY, "discord_notification_failed")
                .addKeyValue(USER_ID_FINGERPRINT_KEY, userIdFingerprint)
                .addKeyValue("failureCode", failureCode)
                .addKeyValue("exceptionType", exceptionType)
                .log("[Discord] 개인 메시지 발송 실패.");
    }

}
