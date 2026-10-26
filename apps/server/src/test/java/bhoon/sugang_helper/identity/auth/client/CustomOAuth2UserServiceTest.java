package bhoon.sugang_helper.identity.auth.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import bhoon.sugang_helper.identity.auth.service.OAuth2AuthenticatedUser;
import bhoon.sugang_helper.identity.user.dto.result.AuthenticatedUser;
import bhoon.sugang_helper.identity.user.enums.Role;
import bhoon.sugang_helper.identity.user.exception.OAuthAccountProvisioningException;
import bhoon.sugang_helper.identity.user.service.OAuthAccountProvisioner;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    private static final String EMAIL_ATTRIBUTE = "email";

    private static final String EMAIL = "rejoined@example.com";
    private static final String NAME = "사용자";
    private static final String SUBJECT = "google-subject";
    private static final String ISSUER = "https://accounts.google.com";

    @Mock
    private OAuthAccountProvisioner accountProvisioner;

    @Test
    void mapsProviderIdentityToTheAuthenticatedPrincipal() {
        OAuth2User delegate = delegateUser(Map.of(EMAIL_ATTRIBUTE, EMAIL, "name", NAME, "sub", SUBJECT));
        given(accountProvisioner.findOrCreate(ISSUER, SUBJECT, EMAIL, NAME))
                .willReturn(new AuthenticatedUser(1L, "original@example.com", Role.ADMIN.getKey()));

        OAuth2User result = serviceReturning(delegate).loadUser(oauth2UserRequest());

        assertThat(result.getName()).isEqualTo(SUBJECT);
        assertThat(result.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly(Role.ADMIN.getKey());
        assertThat(result).isInstanceOf(OAuth2AuthenticatedUser.class);
        OAuth2AuthenticatedUser principal = (OAuth2AuthenticatedUser) result;
        assertThat(principal.getUserId()).isEqualTo(1L);
        assertThat(principal.getEmail()).isEqualTo("original@example.com");
        assertThat(principal.getRoleKey()).isEqualTo(Role.ADMIN.getKey());
        verify(accountProvisioner).findOrCreate(ISSUER, SUBJECT, EMAIL, NAME);
    }

    @Test
    void convertsAccountBindingConflictToAnOAuthAuthenticationFailure() {
        OAuth2User delegate = delegateUser(Map.of(EMAIL_ATTRIBUTE, EMAIL, "name", NAME, "sub", "different-subject"));
        given(accountProvisioner.findOrCreate(ISSUER, "different-subject", EMAIL, NAME))
                .willThrow(new OAuthAccountProvisioningException(
                        "oauth_identity_conflict", "OAuth identity is already bound"));

        assertThatThrownBy(() -> serviceReturning(delegate).loadUser(oauth2UserRequest()))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("already bound");
    }

    @Test
    void rejectsMissingProviderAttributesBeforeProvisioningAnAccount() {
        OAuth2User delegate = delegateUser(Map.of("name", NAME, "sub", SUBJECT));

        assertThatThrownBy(() -> serviceReturning(delegate).loadUser(oauth2UserRequest()))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining(EMAIL_ATTRIBUTE);

        verifyNoInteractions(accountProvisioner);
    }

    private CustomOAuth2UserService serviceReturning(OAuth2User delegateUser) {
        return new CustomOAuth2UserService(accountProvisioner) {
            @Override
            protected OAuth2User getDelegateUser(OAuth2UserRequest userRequest) {
                return delegateUser;
            }
        };
    }

    private OAuth2User delegateUser(Map<String, Object> attributes) {
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(Role.USER.getKey())), attributes, "sub");
    }

    private OAuth2UserRequest oauth2UserRequest() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("google")
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/login/oauth2/code/google")
                .scope("profile", EMAIL_ATTRIBUTE)
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
