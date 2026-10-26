package bhoon.sugang_helper.common.security.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import bhoon.sugang_helper.identity.auth.service.SessionPrincipal;
import bhoon.sugang_helper.identity.user.dto.result.AuthenticatedUser;
import bhoon.sugang_helper.identity.user.service.UserIdentityQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.session.web.http.HttpSessionIdResolver;

@ExtendWith(MockitoExtension.class)
class SessionAuthenticationFilterTest {

    private static final String EMAIL = "user@example.com";
    private static final String USER_ROLE = "ROLE_USER";
    private static final String ADMIN_ROLE = "ROLE_ADMIN";
    @Mock private UserIdentityQueryService userIdentityQuery;
    @Mock private SecurityContextRepository contexts;
    @Mock private HttpSessionIdResolver sessionIds;
    @Mock private FilterChain chain;
    private SessionAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new SessionAuthenticationFilter(userIdentityQuery, contexts, sessionIds, new ObjectMapper());
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void arbitraryBearerHeaderDoesNotCreateAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer ignored-legacy-credential");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(userIdentityQuery, contexts, sessionIds);
        verify(chain).doFilter(request, response);
    }

    @Test
    void everyRequestUsesTheActiveUserAndCurrentRoleAndRollsTheCookieOnly() throws Exception {
        MockHttpServletRequest request = authenticatedRequest(ADMIN_ROLE);
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.getSession().setMaxInactiveInterval(2_592_000);
        given(userIdentityQuery.findAuthenticatedUser(1L, EMAIL))
                .willReturn(Optional.of(new AuthenticatedUser(1L, EMAIL, USER_ROLE)));

        filter.doFilter(request, response, chain);

        ArgumentCaptor<SecurityContext> current = ArgumentCaptor.forClass(SecurityContext.class);
        verify(contexts).saveContext(current.capture(), eq(request), eq(response));
        assertThat(current.getValue().getAuthentication().getAuthorities())
                .extracting("authority").containsExactly(USER_ROLE);
        verify(sessionIds).setSessionId(request, response, request.getSession().getId());
        assertThat(request.getSession().getMaxInactiveInterval()).isEqualTo(2_592_000);
        verify(chain).doFilter(request, response);
    }

    @Test
    void withdrawnOrRejoinedUserCannotReuseTheOldIdentity() throws Exception {
        MockHttpServletRequest request = authenticatedRequest(USER_ROLE);
        MockHttpSession session = (MockHttpSession) request.getSession();
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(userIdentityQuery.findAuthenticatedUser(1L, EMAIL)).willReturn(Optional.empty());

        filter.doFilter(request, response, chain);

        assertThat(session.isInvalid()).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(sessionIds).expireSession(request, response);
        verify(contexts, never()).saveContext(any(), any(), any());
        verify(chain).doFilter(request, response);
    }

    @Test
    void databaseFailureFailsClosedWithoutDeletingTheStoredSession() throws Exception {
        MockHttpServletRequest request = authenticatedRequest(USER_ROLE);
        MockHttpSession session = (MockHttpSession) request.getSession();
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(userIdentityQuery.findAuthenticatedUser(1L, EMAIL))
                .willThrow(new DataAccessResourceFailureException("unavailable"));

        filter.doFilter(request, response, chain);

        assertUnavailable(response);
        assertThat(session.isInvalid()).isFalse();
        SecurityContext stored = (SecurityContext) session
                .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(stored.getAuthentication().getPrincipal()).isEqualTo(new SessionPrincipal(1L, EMAIL));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(chain, sessionIds, contexts);
    }

    @Test
    void redisReadFailureFailsClosedWithoutClearingTheBrowserCookie() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        SecurityContextHolder.setDeferredContext(() -> {
            throw new RedisConnectionFailureException("unavailable");
        });

        filter.doFilter(request, response, chain);

        assertUnavailable(response);
        verifyNoInteractions(userIdentityQuery, chain, sessionIds, contexts);
    }

    private void assertUnavailable(MockHttpServletResponse response) throws Exception {
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(new ObjectMapper().readTree(response.getContentAsString()).path("code").asText()).isEqualTo("G001");
        assertThat(response.getHeaders("Set-Cookie")).isEmpty();
    }

    private MockHttpServletRequest authenticatedRequest(String role) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new SessionPrincipal(1L, EMAIL).authentication(role));
        SecurityContextHolder.setContext(context);
        request.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return request;
    }
}
