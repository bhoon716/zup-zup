package bhoon.sugang_helper.common.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    private static final String EMAIL = "rejoined@example.com";
    private static final String GOOGLE_SUBJECT = "google-subject";

    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void existingGoogleIdentityLogsInWithoutCreatingAnotherAccount() {
        User existingUser = User.builder()
                .id(1L)
                .email(EMAIL)
                .name("기존 사용자")
                .role(Role.USER)
                .build();
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(existingUser));
        OAuth2User delegateUser = new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(Role.USER.getKey())),
                Map.of("email", EMAIL, "name", "기존 사용자", "sub", GOOGLE_SUBJECT), "sub");
        CustomOAuth2UserService service = serviceReturning(delegateUser);

        OAuth2User result = service.loadUser(oauth2UserRequest());

        assertThat(result.getName()).isEqualTo(GOOGLE_SUBJECT);
        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(Role.USER.getKey());
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.<User>any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void withdrawnGoogleIdentityCreatesANewAccountInsteadOfRestoringTheSoftDeletedAccount() {
        User withdrawnUser = User.builder()
                .id(1L)
                .email(EMAIL)
                .name("기존 사용자")
                .role(Role.USER)
                .build();
        withdrawnUser.withdraw();
        User rejoinedUser = User.builder()
                .id(2L)
                .email(EMAIL)
                .name("새 사용자")
                .role(Role.USER)
                .build();
        given(userRepository.findByEmail(EMAIL))
                .willReturn(Optional.<User>empty())
                .willReturn(Optional.of(rejoinedUser));
        given(userRepository.save(org.mockito.ArgumentMatchers.<User>any())).willReturn(rejoinedUser);

        OAuth2User delegateUser = new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(Role.USER.getKey())),
                Map.of("email", EMAIL, "name", "새 사용자", "sub", GOOGLE_SUBJECT), "sub");
        CustomOAuth2UserService service = serviceReturning(delegateUser);

        OAuth2User result = service.loadUser(oauth2UserRequest());

        assertThat(withdrawnUser.isDeleted()).isTrue();
        assertThat(result.getName()).isEqualTo(GOOGLE_SUBJECT);
        verify(userRepository).save(org.mockito.ArgumentMatchers.<User>argThat(user -> user.getEmail().equals(EMAIL)
                && user.getName().equals("새 사용자") && user.getRole() == Role.USER));
        verify(eventPublisher).publishEvent(new UserRegisteredEvent(2L, EMAIL));
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
                .scope("profile", "email")
                .authorizationUri("https://accounts.example/authorize")
                .tokenUri("https://accounts.example/token")
                .userInfoUri("https://accounts.example/userinfo")
                .userNameAttributeName("sub")
                .clientName("Google")
                .build();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "access-token", Instant.now(), Instant.now().plusSeconds(60));
        return new OAuth2UserRequest(registration, accessToken);
    }
}
