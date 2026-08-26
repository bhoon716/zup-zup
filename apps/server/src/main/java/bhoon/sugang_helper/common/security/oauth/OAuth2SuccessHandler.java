package bhoon.sugang_helper.common.security.oauth;

import bhoon.sugang_helper.auth.application.AuthService;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.security.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final AuthService authService;

    @Value("${app.oauth2.success-redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        if (!(authentication.getPrincipal() instanceof OAuth2AuthenticatedUser user)) {
            throw new CustomException(ErrorCode.USER_UNAUTHORIZED);
        }

        String accessToken = jwtProvider.createAccessToken(user.getUserId(), user.getEmail(), user.getRoleKey());
        String refreshToken = jwtProvider.createRefreshToken(user.getUserId(), user.getEmail());

        // 인증 쿠키는 공통 서비스 로직으로 설정한다.
        authService.addRefreshTokenCookie(response, refreshToken);

        // 세션에는 JWT 원문 대신 Spring Security 인증 주체와 권한만 저장한다.
        authService.saveSessionAuthentication(request, response, accessToken);

        log.info("[OAuth2] Social login successful. userId={}, authenticationSaved=true", user.getUserId());
        getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }
}
