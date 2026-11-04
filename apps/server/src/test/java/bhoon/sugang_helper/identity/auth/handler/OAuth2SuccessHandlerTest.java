package bhoon.sugang_helper.identity.auth.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.identity.auth.service.AuthService;
import bhoon.sugang_helper.identity.auth.service.OAuth2AuthenticatedUser;
import bhoon.sugang_helper.identity.user.enums.Role;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    private static final String EMAIL = "test@example.com";
    @Mock
    private AuthService authService;
    @InjectMocks
    private OAuth2SuccessHandler successHandler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(successHandler, "redirectUri", "https://web.example.com");
    }

    @Test
    void providerIdentityStartsAServerSessionBeforeRedirect() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication authentication = mock(Authentication.class);
        var user = new OAuth2AuthenticatedUser(1L, EMAIL, Role.USER.getKey(),
                Map.of("email", EMAIL, "sub", "google-subject"), "sub");
        given(authentication.getPrincipal()).willReturn(user);

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(authService).login(request, response, 1L, EMAIL);
        assertThat(response.getRedirectedUrl()).isEqualTo("https://web.example.com");
        assertThat(response.getHeaders("Set-Cookie")).isEmpty();
    }
}
