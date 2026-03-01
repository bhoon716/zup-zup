package bhoon.sugang_helper.common.security.oauth;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.security.constant.SecurityConstant;
import bhoon.sugang_helper.common.security.jwt.JwtProvider;
import bhoon.sugang_helper.domain.auth.service.AuthService;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final AuthService authService;

    @Value("${app.oauth2.success-redirect-uri}")
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

        // 인증 쿠키는 공통 서비스 로직으로 설정한다.
        authService.addRefreshTokenCookie(response, refreshToken);

        // 토큰은 서버 세션에 저장해 브라우저 노출을 피한다.
        request.getSession().setAttribute("ACCESS_TOKEN", accessToken);
        request.getSession().setAttribute("REFRESH_TOKEN", refreshToken);

        log.info("소셜 로그인(OAuth2) 성공: email={}, 세션 저장 완료", user.getEmail());
        getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }
}
