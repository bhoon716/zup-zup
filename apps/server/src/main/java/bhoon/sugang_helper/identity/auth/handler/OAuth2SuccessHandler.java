package bhoon.sugang_helper.identity.auth.handler;

import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.identity.auth.service.AuthService;
import bhoon.sugang_helper.identity.auth.service.OAuth2AuthenticatedUser;
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

    private final AuthService authService;

    @Value("${app.oauth2.success-redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        if (!(authentication.getPrincipal() instanceof OAuth2AuthenticatedUser user)) {
            throw new CustomException(ErrorCode.USER_UNAUTHORIZED);
        }

        authService.login(request, response, user.getUserId(), user.getEmail());

        logOauthLoginSucceeded(user.getUserId());
        getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }

    private void logOauthLoginSucceeded(Long userId) {
        log.atInfo().addKeyValue("event", "oauth_login_succeeded")
                .addKeyValue("userId", userId)
                .addKeyValue("authenticationSaved", true)
                .log("[OAuth2] 외부 계정 로그인 완료.");
    }

}
