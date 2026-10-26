package bhoon.sugang_helper.identity.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bhoon.sugang_helper.SugangHelperApplication;
import bhoon.sugang_helper.identity.user.entity.User;
import bhoon.sugang_helper.identity.user.enums.Role;
import bhoon.sugang_helper.identity.user.exception.OAuthAccountProvisioningException;
import bhoon.sugang_helper.identity.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@Import(UserOAuthAccountService.class)
@ContextConfiguration(classes = SugangHelperApplication.class)
class OAuthIdentityBindingIntegrationTest {

    private static final String EMAIL = "admin@example.com";
    private static final String ISSUER = "https://accounts.google.com";
    private static final String ORIGINAL_SUBJECT = "original-admin-subject";

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserOAuthAccountService userOAuthAccountService;

    @Test
    void reassignedEmailWithAnotherSubjectCannotInheritPersistedAdminAccount() {
        User admin = userRepository.saveAndFlush(User.builder()
                .email(EMAIL)
                .name("관리자")
                .role(Role.ADMIN)
                .oauthIssuer(ISSUER)
                .oauthSubject(ORIGINAL_SUBJECT)
                .build());

        assertThatThrownBy(() -> userOAuthAccountService.findOrCreate(
                ISSUER, "reassigned-email-subject", EMAIL, "다른 사용자"))
                .isInstanceOf(OAuthAccountProvisioningException.class)
                .hasMessageContaining("already bound");

        assertThat(userRepository.count()).isEqualTo(1L);
        User persistedAdmin = userRepository.findById(admin.getId()).orElseThrow();
        assertThat(persistedAdmin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(persistedAdmin.getOauthSubject()).isEqualTo(ORIGINAL_SUBJECT);
    }
}
