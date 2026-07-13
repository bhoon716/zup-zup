package bhoon.sugang_helper.notification.infra;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.security.util.SensitiveDataRedactor;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class DiscordNotificationSender implements NotificationSender {

    private final RestClient restClient;

    @Value("${app.discord.bot-token:}")
    private String botToken;

    public DiscordNotificationSender() {
        this.restClient = RestClient.builder()
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
        if (botToken == null || botToken.isBlank() || botToken.equals("<DISCORD_BOT_TOKEN>")) {
            log.warn("[Discord] Skipping notification as bot token is not configured.");
            return;
        }

        String userId = target.getRecipient();
        String userIdFingerprint = SensitiveDataRedactor.fingerprint(userId);
        try {
            Map<String, Object> channelResponse = restClient.post()
                    .uri("/users/@me/channels")
                    .header("Authorization", "Bot " + botToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("recipient_id", userId))
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (channelResponse == null || !channelResponse.containsKey("id")) {
                throw new RuntimeException("디스코드 개인 메시지 채널 생성에 실패했습니다.");
            }

            String channelId = (String) channelResponse.get("id");

            String content = String.format("**%s**\n\n%s", title, message);
            Map<String, Object> messagePayload = new HashMap<>();
            messagePayload.put("content", content);
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                messagePayload.put("nonce", idempotencyKey);
                messagePayload.put("enforce_nonce", true);
            }
            restClient.post()
                    .uri("/channels/{channelId}/messages", channelId)
                    .header("Authorization", "Bot " + botToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(messagePayload)
                    .retrieve()
                    .toEntity(String.class);

            log.info("[Discord] Dispatched private message notification. userIdFingerprint={}", userIdFingerprint);
        } catch (Exception e) {
            log.error("[Discord] Private message dispatch failed. userIdFingerprint={}, failureCode={}, exceptionType={}",
                    userIdFingerprint,
                    ErrorCode.DISCORD_SEND_ERROR.getCode(), SensitiveDataRedactor.exceptionType(e));
            throw new CustomException(ErrorCode.DISCORD_SEND_ERROR);
        }
    }
}
