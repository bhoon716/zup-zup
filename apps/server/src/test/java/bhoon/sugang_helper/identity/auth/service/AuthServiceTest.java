package bhoon.sugang_helper.identity.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import bhoon.sugang_helper.common.config.SecurityContextRepositoryConfig;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.identity.user.dto.result.AuthenticatedUser;
import bhoon.sugang_helper.identity.user.service.UserIdentityQueryService;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.session.web.http.DefaultCookieSerializer;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String EMAIL = "test@example.com";
    @Mock
    private UserIdentityQueryService userIdentityQuery;
    private AuthService authService;
    private CsrfTokenRepository csrfTokens;

    @BeforeEach
    void setUp() {
        SecurityContextRepositoryConfig config = new SecurityContextRepositoryConfig();
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        config.persistentSessionCookie().customize(serializer);
        csrfTokens = config.csrfTokenRepository();
        authService = new AuthService(userIdentityQuery, config.securityContextRepository(), csrfTokens,
                config.httpSessionIdResolver(serializer));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginRotatesSessionIdAndCsrfAndUsesTheCurrentDatabaseRole() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        CsrfToken oldCsrf = csrfTokens.generateToken(request);
        csrfTokens.saveToken(oldCsrf, request, response);
        String oldSessionId = request.getSession().getId();
        given(userIdentityQuery.findAuthenticatedUser(1L, EMAIL))
                .willReturn(Optional.of(new AuthenticatedUser(1L, EMAIL, "ROLE_ADMIN")));

        authService.login(request, response, 1L, EMAIL);

        assertThat(request.getSession().getId()).isNotEqualTo(oldSessionId);
        assertThat(csrfTokens.loadToken(request)).isNull();
        SecurityContext context = (SecurityContext) request.getSession()
                .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(context.getAuthentication().getPrincipal()).isEqualTo(new SessionPrincipal(1L, EMAIL));
        assertThat(context.getAuthentication().getName()).isEqualTo(EMAIL);
        assertThat(context.getAuthentication().getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
        assertThat(SecurityContextHolder.getContext()).isSameAs(context);
    }

    @Test
    void logoutEndsOnlyTheCurrentSessionAndExpiresItsCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession current = (MockHttpSession) request.getSession();
        MockHttpSession otherDevice = new MockHttpSession();
        SecurityContextHolder.getContext().setAuthentication(new SessionPrincipal(1L, EMAIL).authentication("ROLE_USER"));

        authService.logout(request, response);

        assertThat(current.isInvalid()).isTrue();
        assertThat(otherDevice.isInvalid()).isFalse();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getHeaders("Set-Cookie")).singleElement()
                .satisfies(cookie -> assertThat(cookie).contains("SESSION=", "Max-Age=0", "HttpOnly", "SameSite=Lax"));
    }

    @Test
    void withdrawnOrRejoinedIdentityCannotStartASession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(userIdentityQuery.findAuthenticatedUser(1L, EMAIL)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request, response, 1L, EMAIL))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTHENTICATION_REQUIRED);
        assertThat(request.getSession(false)).isNull();
    }

    @Test
    void temporaryDatabaseFailureDoesNotDestroyAnExistingSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession session = (MockHttpSession) request.getSession();
        String originalId = session.getId();
        given(userIdentityQuery.findAuthenticatedUser(1L, EMAIL))
                .willThrow(new DataAccessResourceFailureException("database unavailable"));

        assertThatThrownBy(() -> authService.login(request, response, 1L, EMAIL))
                .isInstanceOf(DataAccessResourceFailureException.class);
        assertThat(session.isInvalid()).isFalse();
        assertThat(session.getId()).isEqualTo(originalId);
        assertThat(response.getHeaders("Set-Cookie")).isEmpty();
    }
}
