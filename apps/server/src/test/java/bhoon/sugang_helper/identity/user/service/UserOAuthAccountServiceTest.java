package bhoon.sugang_helper.identity.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.identity.events.UserRegisteredEvent;
import bhoon.sugang_helper.identity.user.dto.result.AuthenticatedUser;
import bhoon.sugang_helper.identity.user.entity.User;
import bhoon.sugang_helper.identity.user.enums.Role;
import bhoon.sugang_helper.identity.user.exception.OAuthAccountProvisioningException;
import bhoon.sugang_helper.identity.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class UserOAuthAccountServiceTest {

    private static final String EMAIL = "rejoined@example.com";
    private static final String ISSUER = "https://accounts.google.com";
    private static final String SUBJECT = "google-subject";

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserOAuthAccountService service;

    @Test
    @DisplayName("기존 OAuth 계정은 재할당된 이메일로 조회하지 않는다")
    void existingOAuthIdentityDoesNotResolveByReassignedEmail() {
        // given
        User existingUser = User.builder()
                .id(1L)
                .email("original@example.com")
                .name("기존 사용자")
                .role(Role.USER)
                .oauthIssuer(ISSUER)
                .oauthSubject(SUBJECT)
                .build();
        given(userRepository.findByOauthIssuerAndOauthSubjectAndDeletedAtIsNull(ISSUER, SUBJECT))
                .willReturn(Optional.of(existingUser));

        // when
        AuthenticatedUser result = service.findOrCreate(ISSUER, SUBJECT, EMAIL, "새 이름");

        // then
        assertThat(result).isEqualTo(new AuthenticatedUser(1L, "original@example.com", Role.USER.getKey()));
        verify(userRepository, never()).findByEmail(EMAIL);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("다른 외부 식별자는 기존 OAuth 계정을 상속할 수 없다")
    void differentSubjectCannotInheritAnAccountWithAnotherOAuthIdentity() {
        // given
        User admin = User.builder()
                .id(1L)
                .email(EMAIL)
                .name("관리자")
                .role(Role.ADMIN)
                .oauthIssuer(ISSUER)
                .oauthSubject("original-admin-subject")
                .build();
        given(userRepository.findByOauthIssuerAndOauthSubjectAndDeletedAtIsNull(ISSUER, "attacker-subject"))
                .willReturn(Optional.empty());
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(admin));

        // when & then
        assertThatThrownBy(() -> service.findOrCreate(ISSUER, "attacker-subject", EMAIL, "다른 사용자"))
                .isInstanceOf(OAuthAccountProvisioningException.class)
                .hasMessageContaining("already bound");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("기존 계정은 한 번만 연결하고 원래 권한을 유지한다")
    void legacyAccountBindsOnceAndKeepsItsExistingRole() {
        // given
        User legacyUser = User.builder()
                .id(2L)
                .email(EMAIL)
                .name("기존 사용자")
                .role(Role.USER)
                .build();
        given(userRepository.findByOauthIssuerAndOauthSubjectAndDeletedAtIsNull(ISSUER, SUBJECT))
                .willReturn(Optional.empty());
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(legacyUser));

        // when
        AuthenticatedUser result = service.findOrCreate(ISSUER, SUBJECT, EMAIL, "기존 사용자");

        // then
        assertThat(legacyUser.getOauthIssuer()).isEqualTo(ISSUER);
        assertThat(legacyUser.getOauthSubject()).isEqualTo(SUBJECT);
        assertThat(legacyUser.getRole()).isEqualTo(Role.USER);
        assertThat(result.roleKey()).isEqualTo(Role.USER.getKey());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("기존 OAuth 관리자는 관리자 권한을 유지한다")
    void existingOAuthAdminKeepsAdministratorAuthority() {
        // given
        User admin = User.builder()
                .id(3L)
                .email(EMAIL)
                .role(Role.ADMIN)
                .oauthIssuer(ISSUER)
                .oauthSubject(SUBJECT)
                .build();
        given(userRepository.findByOauthIssuerAndOauthSubjectAndDeletedAtIsNull(ISSUER, SUBJECT))
                .willReturn(Optional.of(admin));

        // when & then
        assertThat(service.findOrCreate(ISSUER, SUBJECT, EMAIL, "관리자").roleKey())
                .isEqualTo(Role.ADMIN.getKey());
    }

    @Test
    @DisplayName("새 계정은 OAuth 식별자를 저장하고 가입 이벤트를 발행한다")
    void newAccountStoresOAuthIdentityAndPublishesRegistrationEvent() {
        // given
        User savedUser = User.builder()
                .id(4L)
                .email(EMAIL)
                .name("새 사용자")
                .role(Role.USER)
                .oauthIssuer(ISSUER)
                .oauthSubject(SUBJECT)
                .build();
        given(userRepository.findByOauthIssuerAndOauthSubjectAndDeletedAtIsNull(ISSUER, SUBJECT))
                .willReturn(Optional.empty());
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // when
        AuthenticatedUser result = service.findOrCreate(ISSUER, SUBJECT, EMAIL, "새 사용자");

        // then
        assertThat(result.id()).isEqualTo(4L);
        verify(userRepository).save(ArgumentMatchers.<User>argThat(user ->
                ISSUER.equals(user.getOauthIssuer())
                        && SUBJECT.equals(user.getOauthSubject())
                        && user.getRole() == Role.USER));
        verify(eventPublisher).publishEvent(new UserRegisteredEvent(4L, EMAIL));
    }
}
