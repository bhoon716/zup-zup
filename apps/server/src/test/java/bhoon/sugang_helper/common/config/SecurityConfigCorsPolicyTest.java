package bhoon.sugang_helper.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;

class SecurityConfigCorsPolicyTest {

    @Test
    void credentialsUseOnlyExactConfiguredOrigins() {
        CorsConfiguration configuration = SecurityConfig.createCorsConfiguration(
                new String[]{"https://zup-zup.com", "https://www.zup-zup.com"}, true);

        assertThat(configuration.getAllowedOrigins())
                .containsExactly("https://zup-zup.com", "https://www.zup-zup.com");
        assertThat(configuration.getAllowedOriginPatterns()).isNull();
        assertThat(configuration.getAllowCredentials()).isTrue();
        assertThat(configuration.getAllowedHeaders()).containsExactly("Content-Type");
    }

    @Test
    void wildcardOriginIsRejectedWhenCredentialsAreEnabled() {
        assertThatIllegalArgumentException().isThrownBy(() -> SecurityConfig.createCorsConfiguration(
                new String[]{"https://*.zup-zup.com"}, true));
    }

    @Test
    void productionPolicyRejectsNonHttpsOrigins() {
        assertThatIllegalArgumentException().isThrownBy(() -> SecurityConfig.createCorsConfiguration(
                new String[]{"http://localhost:3000"}, true));
    }

    @Test
    void corsPolicyRejectsNonHttpOrigins() {
        assertThatIllegalArgumentException().isThrownBy(() -> SecurityConfig.createCorsConfiguration(
                new String[]{"ftp://example.com"}, false));
    }
}
