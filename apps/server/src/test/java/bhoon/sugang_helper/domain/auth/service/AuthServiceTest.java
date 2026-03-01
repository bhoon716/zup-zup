package bhoon.sugang_helper.domain.auth.service;

import static bhoon.sugang_helper.common.security.constant.SecurityConstant.REDIS_REFRESH_TOKEN_PREFIX;
import static bhoon.sugang_helper.common.security.constant.SecurityConstant.REFRESH_TOKEN_COOKIE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.common.security.jwt.JwtProvider;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

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

        User user = User.builder().email(email).role(bhoon.sugang_helper.domain.user.entity.Role.USER).build();
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(jwtProvider.createAccessToken(anyString(), anyString())).willReturn("new-access-token");
        given(jwtProvider.createRefreshToken(anyString())).willReturn("new-refresh-token");
        given(request.getSession(true)).willReturn(session);

        // when
        String result = authService.reissue(request, response);

        // then
        assertThat(result).isEqualTo("new-access-token");
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
}
