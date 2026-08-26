package bhoon.sugang_helper.common.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import bhoon.sugang_helper.SugangHelperApplication;
import bhoon.sugang_helper.user.domain.Role;
import bhoon.sugang_helper.user.domain.User;
import bhoon.sugang_helper.user.infra.UserJpaRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@ContextConfiguration(classes = SugangHelperApplication.class)
class OAuthIdentityBindingIntegrationTest {

    private static final String EMAIL = "admin@example.com";
    private static final String ISSUER = "https://accounts.google.com";
    private static final String ORIGINAL_SUBJECT = "original-admin-subject";
    private static final String REASSIGNED_SUBJECT = "reassigned-email-subject";

    @Autowired
    private UserJpaRepository userRepository;

    @Test
    void reassignedEmailWithAnotherSubjectCannotInheritPersistedAdminAccount() {
        User admin = userRepository.saveAndFlush(User.builder()
                .email(EMAIL)
                .name("관리자")
                .role(Role.ADMIN)
                .oauthIssuer(ISSUER)
                .oauthSubject(ORIGINAL_SUBJECT)
                .build());
        OAuth2User delegateUser = new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(Role.USER.getKey())),
                Map.of("email", EMAIL, "name", "다른 사용자", "sub", REASSIGNED_SUBJECT), "sub");
        CustomOAuth2UserService service = serviceReturning(delegateUser);

        assertThatThrownBy(() -> service.loadUser(oauth2UserRequest()))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("already bound");

        assertThat(userRepository.count()).isEqualTo(1L);
        User persistedAdmin = userRepository.findById(admin.getId()).orElseThrow();
        assertThat(persistedAdmin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(persistedAdmin.getOauthSubject()).isEqualTo(ORIGINAL_SUBJECT);
    }

    private CustomOAuth2UserService serviceReturning(OAuth2User delegateUser) {
        return new CustomOAuth2UserService(userRepository, mock(ApplicationEventPublisher.class)) {
            @Override
            protected OAuth2User getDelegateUser(OAuth2UserRequest userRequest) {
                return delegateUser;
            }
        };
    }

    private OAuth2UserRequest oauth2UserRequest() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("google")
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/login/oauth2/code/google")
                .scope("profile", "email")
                .authorizationUri("https://accounts.example/authorize")
                .tokenUri("https://accounts.example/token")
                .userInfoUri("https://accounts.example/userinfo")
                .userNameAttributeName("sub")
                .issuerUri(ISSUER)
                .clientName("Google")
                .build();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "access-token", Instant.now(), Instant.now().plusSeconds(60));
        return new OAuth2UserRequest(registration, accessToken);
    }
}
