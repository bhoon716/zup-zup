package bhoon.sugang_helper.common.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.auth.application.AuthService;
import bhoon.sugang_helper.common.security.jwt.JwtProvider;
import bhoon.sugang_helper.user.domain.Role;
import bhoon.sugang_helper.user.domain.User;
import bhoon.sugang_helper.user.domain.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    private static final String EMAIL = "test@example.com";

    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthService authService;
    @InjectMocks
    private OAuth2SuccessHandler successHandler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(successHandler, "redirectUri", "https://web.example.com");
    }

    @Test
    void successDoesNotStoreRawJwtAttributesInHttpSession() throws Exception {
        HttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();
        Authentication authentication = mock(Authentication.class);
        OAuth2User oauth2User = mock(OAuth2User.class);
        User user = User.builder().email(EMAIL).role(Role.USER).build();
        given(authentication.getPrincipal()).willReturn(oauth2User);
        given(oauth2User.getAttributes()).willReturn(Map.of("email", EMAIL));
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
        given(jwtProvider.createAccessToken(anyString(), anyString())).willReturn("access-token");
        given(jwtProvider.createRefreshToken(EMAIL)).willReturn("refresh-token");

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(authService).addRefreshTokenCookie(response, "refresh-token");
        verify(authService).saveSessionAuthentication(request, response, "access-token");
        assertThat(((MockHttpServletRequest) request).getSession(false)).isNull();
        assertThat(((MockHttpServletResponse) response).getRedirectedUrl()).isEqualTo("https://web.example.com");
    }
}
