package bhoon.sugang_helper.common.security.oauth;

import java.io.Serial;
import java.util.Collections;
import java.util.Map;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

final class OAuth2AuthenticatedUser extends DefaultOAuth2User {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String email;
    private final String roleKey;

    OAuth2AuthenticatedUser(Long userId, String email, String roleKey, Map<String, Object> attributes,
                            String nameAttributeKey) {
        super(Collections.singleton(new SimpleGrantedAuthority(roleKey)), attributes, nameAttributeKey);
        this.userId = userId;
        this.email = email;
        this.roleKey = roleKey;
    }

    Long getUserId() {
        return userId;
    }

    String getEmail() {
        return email;
    }

    String getRoleKey() {
        return roleKey;
    }
}
