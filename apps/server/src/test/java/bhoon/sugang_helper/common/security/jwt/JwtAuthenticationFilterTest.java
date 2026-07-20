package bhoon.sugang_helper.common.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.user.domain.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String TIMETABLES_PATH = "/api/v1/timetables";
    private static final String VALID_TOKEN = "valid-token";
    private static final String OLD_ACCOUNT_TOKEN = "old-account-token";
    private static final String REJOINED_EMAIL = "rejoined@example.com";
    private static final String SESSION_EXPIRES_AT = "AUTHENTICATION_EXPIRES_AT";
    private static final String SESSION_USER_ID = "AUTHENTICATION_USER_ID";

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Authentication authentication;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtProvider, userRepository);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("토큰이 없어도 필터 체인은 항상 진행된다")
    void doFilter_withoutToken_stillContinuesFilterChain() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(jwtProvider.resolveToken(request)).willReturn(null);

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("유효한 토큰이면 인증 정보를 저장하고 필터 체인을 진행한다")
    void doFilter_withValidToken_setsAuthenticationAndContinuesFilterChain() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", TIMETABLES_PATH);
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(jwtProvider.resolveToken(request)).willReturn(VALID_TOKEN);
        given(jwtProvider.validateToken(VALID_TOKEN)).willReturn(true);
        given(jwtProvider.getUserId(VALID_TOKEN)).willReturn(1L);
        given(jwtProvider.getAuthentication(VALID_TOKEN)).willReturn(authentication);
        given(authentication.getName()).willReturn("tester@example.com");
        given(userRepository.existsByIdAndEmailAndDeletedAtIsNull(1L, "tester@example.com")).willReturn(true);

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
    }

    @Test
    @DisplayName("탈퇴 전 Bearer token은 같은 이메일의 새 계정으로 인증되지 않는다")
    void doFilter_withDeletedAccountToken_doesNotAuthenticateNewAccount() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", TIMETABLES_PATH);
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(jwtProvider.resolveToken(request)).willReturn(OLD_ACCOUNT_TOKEN);
        given(jwtProvider.validateToken(OLD_ACCOUNT_TOKEN)).willReturn(true);
        given(jwtProvider.getUserId(OLD_ACCOUNT_TOKEN)).willReturn(1L);
        given(jwtProvider.getAuthentication(OLD_ACCOUNT_TOKEN)).willReturn(authentication);
        given(authentication.getName()).willReturn(REJOINED_EMAIL);
        given(userRepository.existsByIdAndEmailAndDeletedAtIsNull(1L, REJOINED_EMAIL)).willReturn(false);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("리프레시 토큰은 Bearer 인증 정보를 저장하지 않는다")
    void doFilter_withRefreshToken_doesNotAuthenticate() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", TIMETABLES_PATH);
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(jwtProvider.resolveToken(request)).willReturn("refresh-token");
        given(jwtProvider.validateToken("refresh-token")).willReturn(false);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("raw access token 세션 attribute는 더 이상 인증에 사용하지 않는다")
    void doFilter_withLegacyRawSessionToken_doesNotAuthenticate() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", TIMETABLES_PATH);
        request.getSession().setAttribute("ACCESS_TOKEN", "raw-session-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(jwtProvider.resolveToken(request)).willReturn(null);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtProvider, never()).validateToken("raw-session-token");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("만료된 session authentication은 JWT 원문 없이도 무효화되어 refresh를 요구한다")
    void doFilter_withExpiredSessionAuthentication_invalidatesSession() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        given(jwtProvider.resolveToken(request)).willReturn(null);
        given(request.getSession(false)).willReturn(session);
        given(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).willReturn(new Object());
        given(session.getAttribute(SESSION_EXPIRES_AT)).willReturn(0L);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(session).invalidate();
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("만료된 session에 유효하지 않은 Bearer header가 있어도 session 인증을 우회하지 못한다")
    void doFilter_withExpiredSessionAndInvalidBearer_doesNotAuthenticate() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        given(jwtProvider.resolveToken(request)).willReturn("invalid-token");
        given(jwtProvider.validateToken("invalid-token")).willReturn(false);
        given(request.getSession(false)).willReturn(session);
        given(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).willReturn(new Object());
        given(session.getAttribute(SESSION_EXPIRES_AT)).willReturn(0L);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(session).invalidate();
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("탈퇴한 계정의 기존 HTTP session은 만료 전이어도 무효화한다")
    void doFilter_withDeletedAccountSession_invalidatesSession() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        given(jwtProvider.resolveToken(request)).willReturn(null);
        given(request.getSession(false)).willReturn(session);
        given(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).willReturn(new Object());
        given(session.getAttribute(SESSION_EXPIRES_AT)).willReturn(System.currentTimeMillis() + 60_000L);
        given(session.getAttribute(SESSION_USER_ID)).willReturn(1L);
        given(authentication.getName()).willReturn(REJOINED_EMAIL);
        given(userRepository.existsByIdAndEmailAndDeletedAtIsNull(1L, REJOINED_EMAIL)).willReturn(false);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(session).invalidate();
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("사용자 ID가 없는 배포 전 HTTP session은 만료 전이어도 무효화한다")
    void doFilter_withLegacySessionWithoutUserId_invalidatesSession() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        given(jwtProvider.resolveToken(request)).willReturn(null);
        given(request.getSession(false)).willReturn(session);
        given(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).willReturn(new Object());
        given(session.getAttribute(SESSION_EXPIRES_AT)).willReturn(System.currentTimeMillis() + 60_000L);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(session).invalidate();
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
