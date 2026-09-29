package bhoon.sugang_helper.identity.auth.contract;

import java.io.Serial;
import java.util.Collections;
import java.util.Map;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

public final class OAuth2AuthenticatedUser extends DefaultOAuth2User {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String email;
    private final String roleKey;

    public OAuth2AuthenticatedUser(Long userId, String email, String roleKey, Map<String, Object> attributes,
                                   String nameAttributeKey) {
        super(Collections.singleton(new SimpleGrantedAuthority(roleKey)), attributes, nameAttributeKey);
        this.userId = userId;
        this.email = email;
        this.roleKey = roleKey;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getRoleKey() {
        return roleKey;
    }
}
