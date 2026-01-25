package bhoon.sugang_helper.common.security.oauth;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.common.security.constant.SecurityConstant;
import bhoon.sugang_helper.common.security.jwt.JwtProvider;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final RedisService redisService;
    private final UserRepository userRepository;

    @Value("${app.oauth2.authorized-redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2User userDetails = (OAuth2User) authentication.getPrincipal();
        String email = (String) userDetails.getAttributes().get(SecurityConstant.CLAIM_EMAIL);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String accessToken = jwtProvider.createAccessToken(user.getEmail(), user.getRoleKey());
        String refreshToken = jwtProvider.createRefreshToken(user.getEmail());

        redisService.setValues(SecurityConstant.REDIS_REFRESH_TOKEN_PREFIX + user.getEmail(), refreshToken,
                java.time.Duration.ofMillis(SecurityConstant.REFRESH_TOKEN_COOKIE_MAX_AGE * 1000L));

        addRefreshTokenCookie(response, refreshToken);

        response.addHeader(SecurityConstant.ACCESS_TOKEN_HEADER, SecurityConstant.TOKEN_PREFIX + accessToken);

        log.info("OAuth2 Login Success: email={}", user.getEmail());
        getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(SecurityConstant.REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // 로컬 테스트 및 HTTP 지원을 위해 false로 설정
        cookie.setPath("/");
        cookie.setMaxAge(SecurityConstant.REFRESH_TOKEN_COOKIE_MAX_AGE);
        response.addCookie(cookie);
    }
}
