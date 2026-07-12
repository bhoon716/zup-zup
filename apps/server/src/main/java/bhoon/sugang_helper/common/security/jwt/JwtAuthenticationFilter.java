package bhoon.sugang_helper.common.security.jwt;

import static bhoon.sugang_helper.common.security.constant.SecurityConstant.SESSION_AUTHENTICATION_EXPIRES_AT;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    @SuppressWarnings("NullableProblems")
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 헤더에서 토큰 추출
        String token = jwtProvider.resolveToken(request);
        clearExpiredSessionAuthentication(request);

        if (StringUtils.hasText(token) && jwtProvider.validateToken(token)) {
            Authentication authentication = jwtProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Saved authentication info '{}' to SecurityContext", authentication.getName());
        }

        filterChain.doFilter(request, response);
    }

    private void clearExpiredSessionAuthentication(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        HttpSession session = request.getSession(false);
        if (authentication == null || session == null
                || session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY) == null) {
            return;
        }

        Object expiresAt = session.getAttribute(SESSION_AUTHENTICATION_EXPIRES_AT);
        if (!(expiresAt instanceof Long expiresAtMillis) || expiresAtMillis <= System.currentTimeMillis()) {
            SecurityContextHolder.clearContext();
            session.invalidate();
        }
    }
}
