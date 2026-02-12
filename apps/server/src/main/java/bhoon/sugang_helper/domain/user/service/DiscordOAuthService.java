package bhoon.sugang_helper.domain.user.service;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscordOAuthService {

    private final RestClient restClient = RestClient.create("https://discord.com/api");

    @Value("${app.discord.client-id}")
    private String clientId;

    @Value("${app.discord.client-secret}") // 추가 필요
    private String clientSecret;

    @Value("${app.discord.redirect-uri:http://localhost:8080/api/v1/users/discord/callback}")
    private String redirectUri;

    /**
     * Discord OAuth2 코드를 액세스 토큰으로 교환
     */
    public String exchangeCodeForToken(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/oauth2/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("access_token")) {
                throw new CustomException(ErrorCode.INVALID_INPUT, "Discord 토큰 교환에 실패했습니다.");
            }

            return (String) response.get("access_token");
        } catch (Exception e) {
            log.error("[DiscordOAuth] Exchange error: {}", e.getMessage());
            throw new CustomException(ErrorCode.INVALID_INPUT, "Discord 인증 중 오류가 발생했습니다.");
        }
    }

    /**
     * 액세스 토큰을 사용하여 Discord 유저 ID(Snowflake) 조회
     */
    public String getDiscordUserId(String accessToken) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri("/users/@me")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("id")) {
                throw new CustomException(ErrorCode.INVALID_INPUT, "Discord 유저 정보 조회에 실패했습니다.");
            }

            return (String) response.get("id");
        } catch (Exception e) {
            log.error("[DiscordOAuth] User info error: {}", e.getMessage());
            throw new CustomException(ErrorCode.INVALID_INPUT, "Discord 유저 정보를 가져올 수 없습니다.");
        }
    }
}
