package bhoon.sugang_helper.identity.auth.service;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.identity.user.dto.result.AuthenticatedUser;
import bhoon.sugang_helper.identity.user.service.UserIdentityQueryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.session.web.http.HttpSessionIdResolver;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserIdentityQueryService userIdentityQuery;
    private final SecurityContextRepository securityContextRepository;
    private final CsrfTokenRepository csrfTokenRepository;
    private final HttpSessionIdResolver sessionIdResolver;

    public void login(HttpServletRequest request, HttpServletResponse response, Long userId, String email) {
        AuthenticatedUser user = userIdentityQuery.findAuthenticatedUser(userId, email)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTHENTICATION_REQUIRED));
        HttpSession session = request.getSession(false);
        if (session != null) {
            request.changeSessionId();
        }
        csrfTokenRepository.saveToken(null, request, response);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new SessionPrincipal(user.id(), user.email()).authentication(user.roleKey()));
        securityContextRepository.saveContext(context, request, response);
        SecurityContextHolder.setContext(context);
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        sessionIdResolver.expireSession(request, response);
    }
}
