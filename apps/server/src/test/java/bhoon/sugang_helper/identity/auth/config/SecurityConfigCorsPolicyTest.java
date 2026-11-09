package bhoon.sugang_helper.identity.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;

class SecurityConfigCorsPolicyTest {

    @Test
    @DisplayName("인증 정보는 정확히 설정된 출처에만 허용한다")
    void credentialsUseOnlyExactConfiguredOrigins() {
        // given
        CorsConfiguration configuration = SecurityConfig.createCorsConfiguration(
                new String[]{"https://zup-zup.com", "https://www.zup-zup.com"}, true);

        // when & then
        assertThat(configuration.getAllowedOrigins())
                .containsExactly("https://zup-zup.com", "https://www.zup-zup.com");
        assertThat(configuration.getAllowedOriginPatterns()).isNull();
        assertThat(configuration.getAllowCredentials()).isTrue();
        assertThat(configuration.getAllowedHeaders()).containsExactly("Content-Type", "X-CSRF-TOKEN");
    }

    @Test
    @DisplayName("인증 정보가 활성화되면 와일드카드 출처를 거부한다")
    void wildcardOriginIsRejectedWhenCredentialsAreEnabled() {
        // when & then
        assertThatIllegalArgumentException().isThrownBy(() -> SecurityConfig.createCorsConfiguration(
                new String[]{"https://*.zup-zup.com"}, true));
    }

    @Test
    @DisplayName("운영 출처 정책은 HTTPS가 아닌 주소를 거부한다")
    void productionPolicyRejectsNonHttpsOrigins() {
        // when & then
        assertThatIllegalArgumentException().isThrownBy(() -> SecurityConfig.createCorsConfiguration(
                new String[]{"http://localhost:3000"}, true));
    }

    @Test
    @DisplayName("CORS 출처 정책은 HTTP 계열이 아닌 주소를 거부한다")
    void corsPolicyRejectsNonHttpOrigins() {
        // when & then
        assertThatIllegalArgumentException().isThrownBy(() -> SecurityConfig.createCorsConfiguration(
                new String[]{"ftp://example.com"}, false));
    }
}
