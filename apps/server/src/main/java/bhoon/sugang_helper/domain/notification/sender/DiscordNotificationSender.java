package bhoon.sugang_helper.domain.notification.sender;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
        if (botToken == null || botToken.isBlank() || botToken.equals("<DISCORD_BOT_TOKEN>")) {
            log.warn("[Discord] Skipping notification as bot token is not configured.");
            return;
        }

        String userId = target.getRecipient();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> channelResponse = restClient.post()
                    .uri("/users/@me/channels")
                    .header("Authorization", "Bot " + botToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("recipient_id", userId))
                    .retrieve()
                    .body(Map.class);

            if (channelResponse == null || !channelResponse.containsKey("id")) {
                throw new RuntimeException("디스코드 개인 메시지 채널 생성에 실패했습니다.");
            }

            String channelId = (String) channelResponse.get("id");

            String content = String.format("**%s**\n\n%s", title, message);
            restClient.post()
                    .uri("/channels/{channelId}/messages", channelId)
                    .header("Authorization", "Bot " + botToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("content", content))
                    .retrieve()
                    .toEntity(String.class);

            log.info("[Discord] Dispatched private message notification. userId={}", userId);
        } catch (Exception e) {
            log.error("[Discord] Failed to dispatch private message notification. userId={}, reason={}", userId,
                    e.getMessage());
            throw new CustomException(ErrorCode.DISCORD_SEND_ERROR, "디스코드 알림 발송 실패: " + e.getMessage());
        }
    }
}
