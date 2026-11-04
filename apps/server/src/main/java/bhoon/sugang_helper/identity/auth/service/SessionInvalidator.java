package bhoon.sugang_helper.identity.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SessionInvalidator {

    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    public void invalidateAll(Long userId, String email) {
        for (Session session : sessionRepository.findByPrincipalName(email).values()) {
            Object value = session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            if (!(value instanceof SecurityContext context)) {
                continue;
            }
            Authentication authentication = context.getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof SessionPrincipal principal
                    && userId.equals(principal.userId())) {
                sessionRepository.deleteById(session.getId());
            }
        }
    }
}
