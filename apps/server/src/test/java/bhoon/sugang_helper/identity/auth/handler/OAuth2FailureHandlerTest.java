package bhoon.sugang_helper.identity.auth.handler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

class OAuth2FailureHandlerTest {

    @Test
    @DisplayName("인증 실패는 예외 상세를 노출하지 않고 이동한다")
    void authenticationFailureRedirectsWithoutExposingExceptionDetails() throws Exception {
        // given
        OAuth2FailureHandler failureHandler = new OAuth2FailureHandler();
        ReflectionTestUtils.setField(
                failureHandler, "redirectUri", "https://www.zup-zup.com/login?error=unauthorized");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        failureHandler.onAuthenticationFailure(
                new MockHttpServletRequest(), response, new BadCredentialsException("sensitive provider detail"));

        // then
        assertThat(response.getRedirectedUrl())
                .isEqualTo("https://www.zup-zup.com/login?error=unauthorized")
                .doesNotContain("sensitive provider detail");
    }
}
