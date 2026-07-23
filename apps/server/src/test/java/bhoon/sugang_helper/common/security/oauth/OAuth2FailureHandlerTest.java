package bhoon.sugang_helper.common.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

class OAuth2FailureHandlerTest {

    @Test
    void authenticationFailureRedirectsWithoutExposingExceptionDetails() throws Exception {
        OAuth2FailureHandler failureHandler = new OAuth2FailureHandler();
        ReflectionTestUtils.setField(
                failureHandler, "redirectUri", "https://www.zup-zup.com/login?error=unauthorized");
        MockHttpServletResponse response = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(
                new MockHttpServletRequest(), response, new BadCredentialsException("sensitive provider detail"));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("https://www.zup-zup.com/login?error=unauthorized")
                .doesNotContain("sensitive provider detail");
    }
}
