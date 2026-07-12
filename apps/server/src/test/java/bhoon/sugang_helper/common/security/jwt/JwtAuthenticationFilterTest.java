package bhoon.sugang_helper.common.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Authentication authentication;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtProvider);
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
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/timetables");
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(jwtProvider.resolveToken(request)).willReturn("valid-token");
        given(jwtProvider.validateToken("valid-token")).willReturn(true);
        given(jwtProvider.getAuthentication("valid-token")).willReturn(authentication);
        given(authentication.getName()).willReturn("tester");

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
    }

    @Test
    @DisplayName("리프레시 토큰은 Bearer 인증 정보를 저장하지 않는다")
    void doFilter_withRefreshToken_doesNotAuthenticate() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/timetables");
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
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/timetables");
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
        given(session.getAttribute("AUTHENTICATION_EXPIRES_AT")).willReturn(0L);
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
        given(session.getAttribute("AUTHENTICATION_EXPIRES_AT")).willReturn(0L);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(session).invalidate();
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
