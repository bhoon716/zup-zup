package bhoon.sugang_helper.auth.application;

import static bhoon.sugang_helper.common.security.constant.SecurityConstant.IS_LOGGED_IN_COOKIE_NAME;
import static bhoon.sugang_helper.common.security.constant.SecurityConstant.REFRESH_TOKEN_COOKIE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // Existing cookie contract assertions intentionally repeat attributes.
class AuthServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private SecurityContextRepository securityContextRepository;
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
        given(jwtProvider.validateRefreshToken(refreshToken)).willReturn(true);
        given(jwtProvider.getUserId(refreshToken)).willReturn(USER_ID);

        Authentication authentication = mock(Authentication.class);
        given(authentication.getName()).willReturn(email);
        given(jwtProvider.getAuthentication(refreshToken)).willReturn(authentication);

        User user = User.builder().id(USER_ID).email(email).role(Role.USER).build();
        given(userRepository.findByIdAndEmailAndDeletedAtIsNull(USER_ID, email)).willReturn(Optional.of(user));
        given(jwtProvider.createAccessToken(USER_ID, email, Role.USER.getKey())).willReturn("new-access-token");
        given(jwtProvider.rotateRefreshToken(email, refreshToken)).willReturn("new-refresh-token");
        Authentication sessionAuthentication = mock(Authentication.class);
        given(jwtProvider.getAuthentication("new-access-token")).willReturn(sessionAuthentication);
        given(jwtProvider.getUserId("new-access-token")).willReturn(USER_ID);
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
        ArgumentCaptor<SecurityContext> contextCaptor = ArgumentCaptor.forClass(SecurityContext.class);
        verify(securityContextRepository).saveContext(contextCaptor.capture(), eq(request), eq(response));
        assertThat(contextCaptor.getValue().getAuthentication()).isSameAs(sessionAuthentication);
        verify(request, never()).getSession(true);
    }

    @Test
    @DisplayName("Redis registry가 없거나 rotation이 실패하면 새 토큰을 발급하지 않는다")
    void reissue_rotationRejected_throwsException() {
        // given
        String refreshToken = "valid-refresh-token";
        String email = "test@example.com";
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        given(request.getCookies()).willReturn(new Cookie[]{cookie});
        given(jwtProvider.validateRefreshToken(refreshToken)).willReturn(true);
        given(jwtProvider.getUserId(refreshToken)).willReturn(USER_ID);

        Authentication authentication = mock(Authentication.class);
        given(authentication.getName()).willReturn(email);
        given(jwtProvider.getAuthentication(refreshToken)).willReturn(authentication);
        User user = User.builder().id(USER_ID).email(email).role(Role.USER).build();
        given(userRepository.findByIdAndEmailAndDeletedAtIsNull(USER_ID, email)).willReturn(Optional.of(user));
        given(jwtProvider.rotateRefreshToken(email, refreshToken)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> authService.reissue(request, response))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);
        verify(jwtProvider, never()).createAccessToken(org.mockito.ArgumentMatchers.anyLong(), anyString(), anyString());
        verify(securityContextRepository, never()).saveContext(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("기존 raw JWT 세션 attribute를 제거하고 인증 주체만 저장한다")
    void saveSessionAuthentication_removesLegacyRawAttributes() {
        Authentication authentication = mock(Authentication.class);
        given(request.getSession(false)).willReturn(session);
        given(jwtProvider.getUserId("new-access-token")).willReturn(USER_ID);
        given(jwtProvider.getAuthentication("new-access-token")).willReturn(authentication);
        given(jwtProvider.getExpiration("new-access-token")).willReturn(120_000L);
        long beforeSave = System.currentTimeMillis();

        authService.saveSessionAuthentication(request, response, "new-access-token");

        verify(session).removeAttribute("ACCESS_TOKEN");
        verify(session).removeAttribute("REFRESH_TOKEN");
        ArgumentCaptor<Long> expiresAtCaptor = ArgumentCaptor.forClass(Long.class);
        verify(session).setAttribute(eq("AUTHENTICATION_EXPIRES_AT"), expiresAtCaptor.capture());
        assertThat(expiresAtCaptor.getValue()).isBetween(beforeSave + 120_000L, System.currentTimeMillis() + 120_000L);
        verify(session).setAttribute("AUTHENTICATION_USER_ID", USER_ID);
        ArgumentCaptor<SecurityContext> contextCaptor = ArgumentCaptor.forClass(SecurityContext.class);
        verify(securityContextRepository).saveContext(contextCaptor.capture(), eq(request), eq(response));
        assertThat(contextCaptor.getValue().getAuthentication()).isSameAs(authentication);
    }

    @Test
    @DisplayName("로그아웃 성공시 리프레시 쿠키를 안전하게 삭제한다")
    void logout_success_clearsCookieSafely() {
        // given
        String refreshToken = "valid-refresh-token";
        String email = "test@example.com";
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        given(request.getCookies()).willReturn(new Cookie[]{cookie});
        given(jwtProvider.validateRefreshToken(refreshToken)).willReturn(true);
        given(jwtProvider.getUserId(refreshToken)).willReturn(USER_ID);

        Authentication authentication = mock(Authentication.class);
        given(authentication.getName()).willReturn(email);
        given(jwtProvider.getAuthentication(refreshToken)).willReturn(authentication);
        given(jwtProvider.resolveToken(request)).willReturn("valid-access-token");
        given(jwtProvider.validateToken("valid-access-token")).willReturn(true);
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
        verify(jwtProvider).revokeRefreshToken(email, refreshToken);
        verify(jwtProvider).blacklistAccessToken("valid-access-token");
        verify(session).invalidate();
        verify(session, never()).getAttribute(anyString());
    }

    @Test
    @DisplayName("같은 이메일로 재가입했어도 탈퇴 전 refresh token은 재발급하지 않는다")
    void reissue_deletedAccountTokenIsRejectedBeforeRotation() {
        String refreshToken = "old-refresh-token";
        String email = "rejoined@example.com";
        given(request.getCookies()).willReturn(new Cookie[]{new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken)});
        given(jwtProvider.validateRefreshToken(refreshToken)).willReturn(true);
        given(jwtProvider.getUserId(refreshToken)).willReturn(USER_ID);
        Authentication authentication = mock(Authentication.class);
        given(authentication.getName()).willReturn(email);
        given(jwtProvider.getAuthentication(refreshToken)).willReturn(authentication);
        given(userRepository.findByIdAndEmailAndDeletedAtIsNull(USER_ID, email)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.reissue(request, response))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_UNAUTHORIZED);

        ArgumentCaptor<String> cookieCaptor = ArgumentCaptor.forClass(String.class);
        verify(response, times(2)).addHeader(eq(HttpHeaders.SET_COOKIE), cookieCaptor.capture());
        assertThat(cookieCaptor.getAllValues()).allMatch(cookie -> cookie.contains("Max-Age=0"));
        verify(jwtProvider, never()).rotateRefreshToken(anyString(), anyString());
        verify(jwtProvider, never()).createAccessToken(org.mockito.ArgumentMatchers.anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("토큰 재발급 실패 시 브라우저의 인증 쿠키(refresh_token, is_logged_in) 파기 헤더를 전송한다")
    void reissue_whenRefreshTokenInvalid_clearsAuthCookiesAndThrowsException() {
        // given
        String refreshToken = "invalid-refresh-token";
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        given(request.getCookies()).willReturn(new Cookie[]{cookie});
        given(jwtProvider.validateRefreshToken(refreshToken)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.reissue(request, response))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);

        ArgumentCaptor<String> cookieCaptor = ArgumentCaptor.forClass(String.class);
        verify(response, times(2)).addHeader(eq(HttpHeaders.SET_COOKIE), cookieCaptor.capture());
        java.util.List<String> cookies = cookieCaptor.getAllValues();
        assertThat(cookies.get(0)).contains("refresh_token");
        assertThat(cookies.get(0)).contains("Max-Age=0");
        assertThat(cookies.get(1)).contains(IS_LOGGED_IN_COOKIE_NAME);
        assertThat(cookies.get(1)).contains("Max-Age=0");
    }

    @Test
    @DisplayName("토큰 재발급 중 일시적인 서버 오류가 발생하면 인증 쿠키를 삭제하지 않는다")
    void reissue_whenTransientServerError_preservesAuthCookies() {
        String refreshToken = "valid-refresh-token";
        String email = "test@example.com";
        given(request.getCookies()).willReturn(new Cookie[]{new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken)});
        given(jwtProvider.validateRefreshToken(refreshToken)).willReturn(true);
        given(jwtProvider.getUserId(refreshToken)).willReturn(USER_ID);

        Authentication authentication = mock(Authentication.class);
        given(authentication.getName()).willReturn(email);
        given(jwtProvider.getAuthentication(refreshToken)).willReturn(authentication);
        User user = User.builder().id(USER_ID).email(email).role(Role.USER).build();
        given(userRepository.findByIdAndEmailAndDeletedAtIsNull(USER_ID, email)).willReturn(Optional.of(user));
        given(jwtProvider.rotateRefreshToken(email, refreshToken))
                .willThrow(new IllegalStateException("temporary Redis failure"));

        assertThatThrownBy(() -> authService.reissue(request, response))
                .isInstanceOf(IllegalStateException.class);

        verify(response, never()).addHeader(eq(HttpHeaders.SET_COOKIE), anyString());
    }

    @Test
    @DisplayName("사용자 저장소 조회 실패 시 인증 쿠키를 삭제하지 않는다")
    void reissue_whenUserRepositoryFails_preservesAuthCookies() {
        String refreshToken = "valid-refresh-token";
        String email = "test@example.com";
        given(request.getCookies()).willReturn(new Cookie[]{new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken)});
        given(jwtProvider.validateRefreshToken(refreshToken)).willReturn(true);
        given(jwtProvider.getUserId(refreshToken)).willReturn(USER_ID);

        Authentication authentication = mock(Authentication.class);
        given(authentication.getName()).willReturn(email);
        given(jwtProvider.getAuthentication(refreshToken)).willReturn(authentication);
        given(userRepository.findByIdAndEmailAndDeletedAtIsNull(USER_ID, email))
                .willThrow(new IllegalStateException("temporary database failure"));

        assertThatThrownBy(() -> authService.reissue(request, response))
                .isInstanceOf(IllegalStateException.class);

        verify(response, never()).addHeader(eq(HttpHeaders.SET_COOKIE), anyString());
    }

    @Test
    @DisplayName("세션 저장 실패 시 인증 쿠키 삭제 헤더를 전송하지 않는다")
    void reissue_whenSessionStoreFails_doesNotDeleteAuthCookies() {
        String refreshToken = "valid-refresh-token";
        String email = "test@example.com";
        given(request.getCookies()).willReturn(new Cookie[]{new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken)});
        given(jwtProvider.validateRefreshToken(refreshToken)).willReturn(true);
        given(jwtProvider.getUserId(refreshToken)).willReturn(USER_ID);

        Authentication authentication = mock(Authentication.class);
        given(authentication.getName()).willReturn(email);
        given(jwtProvider.getAuthentication(refreshToken)).willReturn(authentication);
        User user = User.builder().id(USER_ID).email(email).role(Role.USER).build();
        given(userRepository.findByIdAndEmailAndDeletedAtIsNull(USER_ID, email)).willReturn(Optional.of(user));
        given(jwtProvider.rotateRefreshToken(email, refreshToken)).willReturn("new-refresh-token");
        given(jwtProvider.createAccessToken(USER_ID, email, Role.USER.getKey())).willReturn("new-access-token");
        given(jwtProvider.getUserId("new-access-token")).willReturn(USER_ID);
        given(jwtProvider.getAuthentication("new-access-token")).willReturn(authentication);
        given(jwtProvider.rollbackRefreshToken(email, refreshToken, "new-refresh-token")).willReturn(true);
        doThrow(new IllegalStateException("temporary session store failure"))
                .when(securityContextRepository)
                .saveContext(org.mockito.ArgumentMatchers.any(), eq(request), eq(response));

        assertThatThrownBy(() -> authService.reissue(request, response))
                .isInstanceOf(IllegalStateException.class);

        verify(response, never()).addHeader(eq(HttpHeaders.SET_COOKIE), anyString());
        verify(jwtProvider).rollbackRefreshToken(email, refreshToken, "new-refresh-token");
    }

    @Test
    @DisplayName("회전 후 세션 저장 실패가 이전 refresh registry를 소비하지 않는다")
    void reissue_whenSessionStoreFails_restoresStatefulRefreshRegistryForRetry() {
        RedisService statefulRedis = mock(RedisService.class);
        Map<String, String> registry = new HashMap<>();
        String redisKey = "RT:" + "test@example.com";
        org.mockito.Mockito.doAnswer(invocation -> {
            registry.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(statefulRedis).setValues(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
        given(statefulRedis.getValues(org.mockito.ArgumentMatchers.anyString()))
                .willAnswer(invocation -> registry.get(invocation.getArgument(0)));
        given(statefulRedis.compareAndSetValues(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any()))
                .willAnswer(invocation -> {
                    String key = invocation.getArgument(0);
                    String expected = invocation.getArgument(1);
                    if (!expected.equals(registry.get(key))) {
                        return false;
                    }
                    registry.put(key, invocation.getArgument(2));
                    return true;
                });

        JwtProvider statefulJwtProvider = new JwtProvider(statefulRedis);
        ReflectionTestUtils.setField(statefulJwtProvider, "secretKey",
                "testSecretKeytestSecretKeytestSecretKeytestSecretKey");
        ReflectionTestUtils.setField(statefulJwtProvider, "accessTokenExpiration", 1_800_000L);
        ReflectionTestUtils.setField(statefulJwtProvider, "refreshTokenExpiration", 604_800_000L);
        statefulJwtProvider.init();
        AuthService statefulAuthService = new AuthService(statefulJwtProvider, userRepository,
                securityContextRepository);

        String refreshToken = statefulJwtProvider.createRefreshToken(USER_ID, "test@example.com");
        String originalRecord = registry.get(redisKey);
        given(request.getCookies()).willReturn(new Cookie[]{new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken)});
        User user = User.builder().id(USER_ID).email("test@example.com").role(Role.USER).build();
        given(userRepository.findByIdAndEmailAndDeletedAtIsNull(USER_ID, "test@example.com"))
                .willReturn(Optional.of(user));
        AtomicBoolean failSessionStore = new AtomicBoolean(true);
        doAnswer(invocation -> {
            if (failSessionStore.getAndSet(false)) {
                throw new IllegalStateException("temporary session store failure");
            }
            return null;
        }).when(securityContextRepository).saveContext(org.mockito.ArgumentMatchers.any(), eq(request),
                eq(response));

        assertThatThrownBy(() -> statefulAuthService.reissue(request, response))
                .isInstanceOf(IllegalStateException.class);
        assertThat(registry.get(redisKey)).isEqualTo(originalRecord);

        HttpServletResponse retryResponse = mock(HttpServletResponse.class);
        String accessToken = statefulAuthService.reissue(request, retryResponse);

        assertThat(accessToken).isNotBlank();
        assertThat(registry.get(redisKey)).isNotEqualTo(originalRecord);
        verify(retryResponse, times(2)).addHeader(eq(HttpHeaders.SET_COOKIE), anyString());
    }

    @Test
    @DisplayName("쿠키 발행 실패가 replacement refresh registry를 남기지 않는다")
    void reissue_whenCookiePublicationFails_rollsBackRefreshRotation() {
        String refreshToken = "valid-refresh-token";
        String email = "test@example.com";
        given(request.getCookies()).willReturn(new Cookie[]{new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken)});
        given(jwtProvider.validateRefreshToken(refreshToken)).willReturn(true);
        given(jwtProvider.getUserId(refreshToken)).willReturn(USER_ID);
        Authentication authentication = mock(Authentication.class);
        given(authentication.getName()).willReturn(email);
        given(jwtProvider.getAuthentication(refreshToken)).willReturn(authentication);
        User user = User.builder().id(USER_ID).email(email).role(Role.USER).build();
        given(userRepository.findByIdAndEmailAndDeletedAtIsNull(USER_ID, email)).willReturn(Optional.of(user));
        given(jwtProvider.rotateRefreshToken(email, refreshToken)).willReturn("new-refresh-token");
        given(jwtProvider.createAccessToken(USER_ID, email, Role.USER.getKey())).willReturn("new-access-token");
        given(jwtProvider.getUserId("new-access-token")).willReturn(USER_ID);
        given(jwtProvider.getAuthentication("new-access-token")).willReturn(authentication);
        doThrow(new IllegalStateException("response already committed"))
                .when(response).addHeader(eq(HttpHeaders.SET_COOKIE), anyString());
        given(jwtProvider.rollbackRefreshToken(email, refreshToken, "new-refresh-token")).willReturn(true);

        assertThatThrownBy(() -> authService.reissue(request, response))
                .isInstanceOf(IllegalStateException.class);

        verify(jwtProvider).rollbackRefreshToken(email, refreshToken, "new-refresh-token");
    }
}
