package bhoon.sugang_helper.common.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import bhoon.sugang_helper.user.domain.Role;
import bhoon.sugang_helper.user.domain.User;
import bhoon.sugang_helper.user.domain.UserRegisteredEvent;
import bhoon.sugang_helper.user.domain.UserRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    private static final String EMAIL = "rejoined@example.com";
    private static final String EMAIL_ATTRIBUTE = "email";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String SUBJECT_ATTRIBUTE = "sub";
    private static final String EXISTING_USER_NAME = "기존 사용자";
    private static final String GOOGLE_ISSUER = "https://accounts.google.com";
    private static final String GOOGLE_SUBJECT = "google-subject";

    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void existingGoogleIdentityLogsInByIssuerAndSubjectWithoutUsingReassignedEmail() {
        User existingUser = User.builder()
                .id(1L)
                .email("original@example.com")
                .name(EXISTING_USER_NAME)
                .role(Role.USER)
                .oauthIssuer(GOOGLE_ISSUER)
                .oauthSubject(GOOGLE_SUBJECT)
                .build();
        given(userRepository.findByOauthIssuerAndOauthSubjectAndDeletedAtIsNull(GOOGLE_ISSUER, GOOGLE_SUBJECT))
                .willReturn(Optional.of(existingUser));
        OAuth2User delegateUser = new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(Role.USER.getKey())),
                Map.of(EMAIL_ATTRIBUTE, EMAIL, NAME_ATTRIBUTE, EXISTING_USER_NAME,
                        SUBJECT_ATTRIBUTE, GOOGLE_SUBJECT), SUBJECT_ATTRIBUTE);
        CustomOAuth2UserService service = serviceReturning(delegateUser);

        OAuth2User result = service.loadUser(oauth2UserRequest());

        assertThat(result.getName()).isEqualTo(GOOGLE_SUBJECT);
        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(Role.USER.getKey());
        assertThat(result).isInstanceOf(OAuth2AuthenticatedUser.class);
        assertThat(((OAuth2AuthenticatedUser) result).getUserId()).isEqualTo(1L);
        assertThat(((OAuth2AuthenticatedUser) result).getEmail()).isEqualTo("original@example.com");
        verify(userRepository, never()).findByEmail(EMAIL);
        verify(userRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void sameEmailWithDifferentSubjectIsRejectedInsteadOfInheritingExistingAdmin() {
        User admin = User.builder()
                .id(1L)
                .email(EMAIL)
                .name("관리자")
                .role(Role.ADMIN)
                .oauthIssuer(GOOGLE_ISSUER)
                .oauthSubject("original-admin-subject")
                .build();
        given(userRepository.findByOauthIssuerAndOauthSubjectAndDeletedAtIsNull(
                GOOGLE_ISSUER, "attacker-subject")).willReturn(Optional.empty());
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(admin));
        OAuth2User delegateUser = new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(Role.USER.getKey())),
                Map.of(EMAIL_ATTRIBUTE, EMAIL, NAME_ATTRIBUTE, "다른 사용자",
                        SUBJECT_ATTRIBUTE, "attacker-subject"), SUBJECT_ATTRIBUTE);
        CustomOAuth2UserService service = serviceReturning(delegateUser);

        assertThatThrownBy(() -> service.loadUser(oauth2UserRequest()))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("already bound");
        verify(userRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void legacyUserIsBoundOnceAndPreservesTheExistingRole() {
        User legacyUser = User.builder()
                .id(2L)
                .email(EMAIL)
                .name(EXISTING_USER_NAME)
                .role(Role.USER)
                .build();
        given(userRepository.findByOauthIssuerAndOauthSubjectAndDeletedAtIsNull(GOOGLE_ISSUER, GOOGLE_SUBJECT))
                .willReturn(Optional.empty());
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(legacyUser));
        OAuth2User delegateUser = new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(Role.USER.getKey())),
                Map.of(EMAIL_ATTRIBUTE, EMAIL, NAME_ATTRIBUTE, EXISTING_USER_NAME,
                        SUBJECT_ATTRIBUTE, GOOGLE_SUBJECT), SUBJECT_ATTRIBUTE);
        CustomOAuth2UserService service = serviceReturning(delegateUser);

        OAuth2User result = service.loadUser(oauth2UserRequest());

        assertThat(result.getName()).isEqualTo(GOOGLE_SUBJECT);
        assertThat(legacyUser.getOauthIssuer()).isEqualTo(GOOGLE_ISSUER);
        assertThat(legacyUser.getOauthSubject()).isEqualTo(GOOGLE_SUBJECT);
        assertThat(legacyUser.getRole()).isEqualTo(Role.USER);
        verify(userRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void boundAdminRetainsAdminAuthority() {
        User admin = User.builder()
                .id(3L)
                .email(EMAIL)
                .name("관리자")
                .role(Role.ADMIN)
                .oauthIssuer(GOOGLE_ISSUER)
                .oauthSubject(GOOGLE_SUBJECT)
                .build();
        given(userRepository.findByOauthIssuerAndOauthSubjectAndDeletedAtIsNull(GOOGLE_ISSUER, GOOGLE_SUBJECT))
                .willReturn(Optional.of(admin));
        OAuth2User delegateUser = new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(Role.USER.getKey())),
                Map.of(EMAIL_ATTRIBUTE, EMAIL, NAME_ATTRIBUTE, "관리자",
                        SUBJECT_ATTRIBUTE, GOOGLE_SUBJECT), SUBJECT_ATTRIBUTE);

        OAuth2User result = serviceReturning(delegateUser).loadUser(oauth2UserRequest());

        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(Role.ADMIN.getKey());
        assertThat(((OAuth2AuthenticatedUser) result).getRoleKey()).isEqualTo(Role.ADMIN.getKey());
    }

    @Test
    void newUserPersistsUniqueOAuthIdentity() {
        User savedUser = User.builder()
                .id(4L)
                .email(EMAIL)
                .name("새 사용자")
                .role(Role.USER)
                .oauthIssuer(GOOGLE_ISSUER)
                .oauthSubject(GOOGLE_SUBJECT)
                .build();
        given(userRepository.findByOauthIssuerAndOauthSubjectAndDeletedAtIsNull(GOOGLE_ISSUER, GOOGLE_SUBJECT))
                .willReturn(Optional.empty());
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        OAuth2User delegateUser = new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(Role.USER.getKey())),
                Map.of(EMAIL_ATTRIBUTE, EMAIL, NAME_ATTRIBUTE, "새 사용자",
                        SUBJECT_ATTRIBUTE, GOOGLE_SUBJECT), SUBJECT_ATTRIBUTE);

        OAuth2User result = serviceReturning(delegateUser).loadUser(oauth2UserRequest());

        assertThat(((OAuth2AuthenticatedUser) result).getUserId()).isEqualTo(4L);
        verify(userRepository).save(org.mockito.ArgumentMatchers.<User>argThat(user ->
                GOOGLE_ISSUER.equals(user.getOauthIssuer())
                        && GOOGLE_SUBJECT.equals(user.getOauthSubject())
                        && user.getRole() == Role.USER));
        verify(eventPublisher).publishEvent(new UserRegisteredEvent(4L, EMAIL));
    }

    private CustomOAuth2UserService serviceReturning(OAuth2User delegateUser) {
        return new CustomOAuth2UserService(userRepository, eventPublisher) {
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
                .scope("profile", EMAIL_ATTRIBUTE)
                .authorizationUri("https://accounts.example/authorize")
                .tokenUri("https://accounts.example/token")
                .userInfoUri("https://accounts.example/userinfo")
                .userNameAttributeName(SUBJECT_ATTRIBUTE)
                .issuerUri(GOOGLE_ISSUER)
                .clientName("Google")
                .build();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "access-token", Instant.now(), Instant.now().plusSeconds(60));
        return new OAuth2UserRequest(registration, accessToken);
    }
}
