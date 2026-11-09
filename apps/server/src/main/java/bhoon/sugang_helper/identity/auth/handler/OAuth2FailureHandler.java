package bhoon.sugang_helper.identity.auth.handler;

import bhoon.sugang_helper.common.api.security.SensitiveDataRedactor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.oauth2.failure-redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        logOauthLoginFailed(SensitiveDataRedactor.exceptionType(exception));

        getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }

    private void logOauthLoginFailed(String exceptionType) {
        log.atWarn().addKeyValue("event", "oauth_login_failed")
                .addKeyValue("exceptionType", exceptionType)
                .log("[OAuth2] 외부 계정 로그인 실패.");
    }

}
