package bhoon.sugang_helper.identity.auth.service;

import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record SessionPrincipal(Long userId, String email) implements Principal, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public String getName() {
        return email;
    }

    public Authentication authentication(String role) {
        return UsernamePasswordAuthenticationToken.authenticated(this, "", List.of(new SimpleGrantedAuthority(role)));
    }
}
