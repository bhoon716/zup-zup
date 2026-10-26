package bhoon.sugang_helper.common.security.dto.result;

import org.springframework.security.core.Authentication;

public record JwtAuthenticationResult(Long userId, String email, Authentication authentication,
                                      Long expiresAtMillis, String tokenFamily) {
}
