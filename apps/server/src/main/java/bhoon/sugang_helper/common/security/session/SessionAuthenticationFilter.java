package bhoon.sugang_helper.common.security.session;

import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.response.ErrorResponse;
import bhoon.sugang_helper.identity.auth.service.SessionPrincipal;
import bhoon.sugang_helper.identity.user.dto.result.AuthenticatedUser;
import bhoon.sugang_helper.identity.user.service.UserIdentityQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.session.web.http.HttpSessionIdResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private final UserIdentityQueryService userIdentityQuery;
    private final SecurityContextRepository securityContextRepository;
    private final HttpSessionIdResolver sessionIdResolver;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                validateSession(request, response, authentication);
            }
        } catch (RuntimeException exception) {
            SecurityContextHolder.clearContext();
            ErrorResponse error = ErrorResponse.of(ErrorCode.INTERNAL_ERROR, request.getRequestURI());
            response.setStatus(error.getStatus());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("X-Error-Id", error.getCorrelationId());
            log.error("[SESSION_UNAVAILABLE] 세션 확인 실패로 인증 거절. correlationId={} exceptionType={}",
                    error.getCorrelationId(), exception.getClass().getSimpleName());
            objectMapper.writeValue(response.getWriter(), error);
            return;
        }
        chain.doFilter(request, response);
    }

    private void validateSession(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            SecurityContextHolder.clearContext();
            return;
        }
        if (!(authentication.getPrincipal() instanceof SessionPrincipal principal)) {
            invalidate(request, response, session);
            return;
        }
        Optional<AuthenticatedUser> user = userIdentityQuery.findAuthenticatedUser(principal.userId(), principal.email());
        if (user.isEmpty()) {
            invalidate(request, response, session);
            return;
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(principal.authentication(user.get().roleKey()));
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    private void invalidate(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
        SecurityContextHolder.clearContext();
        session.invalidate();
        sessionIdResolver.expireSession(request, response);
    }
}
