package bhoon.sugang_helper.domain.auth.service;

import static bhoon.sugang_helper.common.security.constant.SecurityConstant.REFRESH_TOKEN_COOKIE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.common.security.jwt.JwtProvider;
import bhoon.sugang_helper.domain.user.entity.Role;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RedisService redisService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private jakarta.servlet.http.HttpSession session;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("토큰 재발급 성공")
    void reissue_success() {
        // given
        String refreshToken = "valid_refresh_token";
        String email = "test@example.com";
        User user = User.builder()
                .name("test")
                .email(email)
                .role(Role.USER)
                .build();
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);

        given(request.getCookies()).willReturn(new Cookie[] { cookie });
        given(request.getSession(true)).willReturn(session);
        given(jwtProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtProvider.getAuthentication(refreshToken))
                .willReturn(new UsernamePasswordAuthenticationToken(email, null));
        given(redisService.getValues(anyString())).willReturn(refreshToken);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(jwtProvider.createAccessToken(anyString(), anyString())).willReturn("new_access_token");
        given(jwtProvider.createRefreshToken(anyString())).willReturn("new_refresh_token");

        // when
        String newAccessToken = authService.reissue(request, response);

        // then
        assertThat(newAccessToken).isEqualTo("new_access_token");
        verify(response).addHeader(anyString(), anyString());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 리프레시 토큰 없음")
    void reissue_fail_no_token() {
        // given
        given(request.getCookies()).willReturn(null);

        // when & then
        assertThrows(CustomException.class, () -> authService.reissue(request, response));
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout_success() {
        // given
        String refreshToken = "valid_refresh_token";
        String accessToken = "valid_access_token";
        String email = "test@example.com";
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);

        given(request.getCookies()).willReturn(new Cookie[] { cookie });
        given(jwtProvider.resolveToken(request)).willReturn(accessToken);
        given(jwtProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtProvider.getAuthentication(refreshToken))
                .willReturn(new UsernamePasswordAuthenticationToken(email, null));
        given(jwtProvider.validateToken(accessToken)).willReturn(true);

        // when
        authService.logout(request, response);

        // then
        verify(redisService).deleteValues(anyString());
        verify(redisService).setValues(anyString(), anyString(), any());
        verify(response).addCookie(any(Cookie.class));
    }
}
