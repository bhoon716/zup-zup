package bhoon.sugang_helper.common.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
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
}
