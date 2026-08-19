package bhoon.sugang_helper.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserOAuthIdentityTest {

    @Test
    void withdrawalReleasesOAuthIdentityForAnExplicitRejoin() {
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .name("사용자")
                .role(Role.USER)
                .oauthIssuer("https://accounts.google.com")
                .oauthSubject("google-subject")
                .build();

        user.withdraw();

        assertThat(user.isDeleted()).isTrue();
        assertThat(user.hasOAuthIdentity()).isFalse();
        assertThat(user.getOauthIssuer()).isNull();
        assertThat(user.getOauthSubject()).isNull();
    }
}
