package bhoon.sugang_helper.auth.application;

import static bhoon.sugang_helper.common.security.constant.SecurityConstant.IS_LOGGED_IN_COOKIE_NAME;
import static bhoon.sugang_helper.common.security.constant.SecurityConstant.REDIS_REFRESH_TOKEN_PREFIX;
import static bhoon.sugang_helper.common.security.constant.SecurityConstant.REFRESH_TOKEN_COOKIE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.common.security.jwt.JwtProvider;
import bhoon.sugang_helper.user.domain.Role;
import bhoon.sugang_helper.user.domain.User;
import bhoon.sugang_helper.user.domain.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private RedisService redisService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
    }

    @Test
    @DisplayName("토큰 재발급 성공")
    void reissue_success() {
        // given
        String refreshToken = "valid-refresh-token";
        String email = "test@example.com";
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        given(request.getCookies()).willReturn(new Cookie[]{cookie});
        given(jwtProvider.validateToken(refreshToken)).willReturn(true);

        Authentication authentication = mock(Authentication.class);
        given(authentication.getName()).willReturn(email);
        given(jwtProvider.getAuthentication(refreshToken)).willReturn(authentication);
        given(redisService.getValues(REDIS_REFRESH_TOKEN_PREFIX + email)).willReturn(refreshToken);

        User user = User.builder().email(email).role(Role.USER).build();
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(jwtProvider.createAccessToken(anyString(), anyString())).willReturn("new-access-token");
        given(jwtProvider.createRefreshToken(anyString())).willReturn("new-refresh-token");
        given(request.getSession(true)).willReturn(session);
        ReflectionTestUtils.setField(authService, "refreshCookieSecure", true);

        // when
        String result = authService.reissue(request, response);

        // then
        assertThat(result).isEqualTo("new-access-token");
        ArgumentCaptor<String> cookieCaptor = ArgumentCaptor.forClass(String.class);
        verify(response, times(2)).addHeader(eq(HttpHeaders.SET_COOKIE), cookieCaptor.capture());
        java.util.List<String> cookies = cookieCaptor.getAllValues();
        assertThat(cookies.get(0)).contains("refresh_token");
        assertThat(cookies.get(0)).contains("Secure");
        assertThat(cookies.get(0)).contains("HttpOnly");
        assertThat(cookies.get(0)).contains("SameSite=Lax");
        assertThat(cookies.get(1)).contains(IS_LOGGED_IN_COOKIE_NAME);
        assertThat(cookies.get(1)).contains("Secure");
        assertThat(cookies.get(1)).doesNotContain("HttpOnly");
        assertThat(cookies.get(1)).contains("SameSite=Lax");
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 토큰 불일치")
    void reissue_tokenUnmatched_throwsException() {
        // given
        String refreshToken = "valid-refresh-token";
        String email = "test@example.com";
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        given(request.getCookies()).willReturn(new Cookie[]{cookie});
        given(jwtProvider.validateToken(refreshToken)).willReturn(true);

        Authentication authentication = mock(Authentication.class);
        given(authentication.getName()).willReturn(email);
        given(jwtProvider.getAuthentication(refreshToken)).willReturn(authentication);
        given(redisService.getValues(REDIS_REFRESH_TOKEN_PREFIX + email)).willReturn("different-token");

        // when & then
        assertThatThrownBy(() -> authService.reissue(request, response))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("로그아웃 성공시 리프레시 쿠키를 안전하게 삭제한다")
    void logout_success_clearsCookieSafely() {
        // given
        String refreshToken = "valid-refresh-token";
        String email = "test@example.com";
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        given(request.getCookies()).willReturn(new Cookie[]{cookie});
        given(jwtProvider.validateToken(refreshToken)).willReturn(true);

        Authentication authentication = mock(Authentication.class);
        given(authentication.getName()).willReturn(email);
        given(jwtProvider.getAuthentication(refreshToken)).willReturn(authentication);
        given(request.getSession(false)).willReturn(session);
        ReflectionTestUtils.setField(authService, "refreshCookieSecure", true);

        // when
        authService.logout(request, response);

        // then
        ArgumentCaptor<String> cookieCaptor = ArgumentCaptor.forClass(String.class);
        verify(response, times(2)).addHeader(eq(HttpHeaders.SET_COOKIE), cookieCaptor.capture());
        java.util.List<String> cookies = cookieCaptor.getAllValues();
        assertThat(cookies.get(0)).contains("refresh_token");
        assertThat(cookies.get(0)).contains("Max-Age=0");
        assertThat(cookies.get(0)).contains("Secure");
        assertThat(cookies.get(0)).contains("SameSite=Lax");
        assertThat(cookies.get(1)).contains(IS_LOGGED_IN_COOKIE_NAME);
        assertThat(cookies.get(1)).contains("Max-Age=0");
        assertThat(cookies.get(1)).contains("Secure");
        assertThat(cookies.get(1)).contains("SameSite=Lax");
    }
}
