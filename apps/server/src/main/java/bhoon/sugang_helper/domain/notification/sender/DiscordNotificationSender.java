package bhoon.sugang_helper.domain.notification.sender;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

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
            log.warn("[Discord] Bot token is not configured. Skipping notification.");
            return;
        }

        String userId = target.getRecipient();
        try {
            // 1. Create/Get DM Channel
            @SuppressWarnings("unchecked")
            Map<String, Object> channelResponse = restClient.post()
                    .uri("/users/@me/channels")
                    .header("Authorization", "Bot " + botToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("recipient_id", userId))
                    .retrieve()
                    .body(Map.class);

            if (channelResponse == null || !channelResponse.containsKey("id")) {
                throw new RuntimeException("Failed to create DM channel");
            }

            String channelId = (String) channelResponse.get("id");

            // 2. Send Message
            String content = String.format("**%s**\n\n%s", title, message);
            restClient.post()
                    .uri("/channels/{channelId}/messages", channelId)
                    .header("Authorization", "Bot " + botToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("content", content))
                    .retrieve()
                    .toEntity(String.class);

            log.info("[Discord] Sent DM to userId: {}", userId);
        } catch (Exception e) {
            log.error("[Discord] Failed to send DM to userId: {}. Error: {}", userId, e.getMessage());
            throw new CustomException(ErrorCode.DISCORD_SEND_ERROR, "디스코드 알림 발송 실패: " + e.getMessage());
        }
    }
}
