package bhoon.sugang_helper.common.security.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.common.config.RedisConfig;
import bhoon.sugang_helper.common.config.SecurityConfig;
import bhoon.sugang_helper.common.config.SecurityContextRepositoryConfig;
import bhoon.sugang_helper.common.security.exception.CustomAccessDeniedHandler;
import bhoon.sugang_helper.common.security.exception.CustomAuthenticationEntryPoint;
import bhoon.sugang_helper.common.web.GlobalExceptionHandler;
import bhoon.sugang_helper.identity.auth.client.CustomOAuth2UserService;
import bhoon.sugang_helper.identity.auth.controller.AuthController;
import bhoon.sugang_helper.identity.auth.handler.OAuth2FailureHandler;
import bhoon.sugang_helper.identity.auth.handler.OAuth2SuccessHandler;
import bhoon.sugang_helper.identity.auth.service.AuthService;
import bhoon.sugang_helper.identity.auth.service.OAuth2AuthenticatedUser;
import bhoon.sugang_helper.identity.auth.service.SessionInvalidator;
import bhoon.sugang_helper.identity.user.dto.result.AuthenticatedUser;
import bhoon.sugang_helper.identity.user.service.UserIdentityQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.session.data.redis.RedisIndexedSessionRepository.RedisSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Tag("session")
@Testcontainers(disabledWithoutDocker = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(classes = RedisSessionEndToEndTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.config.location=optional:classpath:/session-e2e-absent.yml",
        "spring.data.redis.password=",
        "spring.data.redis.timeout=500ms",
        "spring.data.redis.connect-timeout=500ms",
        "spring.session.timeout=30d",
        "server.servlet.session.timeout=30d",
        "server.servlet.session.cookie.secure=false",
        "server.servlet.session.cookie.http-only=true",
        "server.servlet.session.cookie.same-site=lax",
        "spring.lifecycle.timeout-per-shutdown-phase=2s",
        "spring.cloud.aws.ses.enabled=false",
        "management.health.mail.enabled=false",
        "app.cors.allowed-origins=http://localhost:3000",
        "app.oauth2.success-redirect-uri=http://localhost:3000",
        "app.oauth2.failure-redirect-uri=http://localhost:3000/login?error=true",
        "spring.security.oauth2.client.registration.google.client-id=session-e2e-client",
        "spring.security.oauth2.client.registration.google.client-secret=session-e2e-not-a-real-secret",
        "spring.security.oauth2.client.registration.google.scope=profile,email",
        "spring.security.oauth2.client.registration.google.authorization-grant-type=authorization_code",
        "spring.security.oauth2.client.provider.google.authorization-uri=http://127.0.0.1/disabled-oauth",
        "spring.security.oauth2.client.provider.google.token-uri=http://127.0.0.1/disabled-token",
        "spring.security.oauth2.client.provider.google.user-info-uri=http://127.0.0.1/disabled-userinfo",
        "spring.security.oauth2.client.provider.google.user-name-attribute=sub"
})
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class RedisSessionEndToEndTest {

    private static final String PROBE = "/api/session-e2e/probe";
    private static final String MUTATION = "/api/session-e2e/mutate";
    private static final String ADMIN = "/api/v1/admin/session-e2e";
    private static final String LOGOUT = "/api/auth/logout";
    private static final String CSRF = "/api/auth/csrf";
    private static final String CONTEXT = HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;
    private static final Duration THIRTY_DAYS = Duration.ofDays(30);

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:6-alpine"))
            .withExposedPorts(6379).waitingFor(Wait.forListeningPort());

    @LocalServerPort private int port;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private RedisIndexedSessionRepository sessions;
    @Autowired private SessionInvalidator invalidator;
    @MockitoBean private UserIdentityQueryService users;
    @MockitoBean private CustomOAuth2UserService oauthUsers;
    private final Map<Long, AuthenticatedUser> accounts = new ConcurrentHashMap<>();

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.data.redis.host", REDIS::getHost);
        properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @BeforeEach
    void configureDatabaseBoundary() {
        accounts.clear();
        when(users.findAuthenticatedUser(anyLong(), anyString())).thenAnswer(invocation -> {
            Long userId = invocation.getArgument(0);
            String email = invocation.getArgument(1);
            return Optional.ofNullable(accounts.get(userId)).filter(user -> user.email().equals(email));
        });
    }

    @AfterEach
    void externalOAuthExchangeWasNeverCalled() {
        verifyNoInteractions(oauthUsers);
    }

    @Test
    @Order(1)
    void independentDeviceSessionsLogoutOnlyTheCurrentDeviceAndStaleSaveCannotReviveIt() throws Exception {
        AuthenticatedUser user = account(11L, "device-user@example.test", "ROLE_USER");
        Browser first = new Browser();
        Browser second = new Browser();
        login(first, user);
        login(second, user);
        String firstId = sessionId(first);
        String secondId = sessionId(second);
        assertThat(firstId).isNotEqualTo(secondId);
        assertThat(sessions.findByPrincipalName(user.email())).containsKeys(firstId, secondId);
        RedisSession staleCopy = stored(firstId);
        assertThat(first.post(LOGOUT, "invalid-token").statusCode()).isEqualTo(403);
        String csrf = csrf(first);

        HttpResponse<String> logout = first.post(LOGOUT, csrf);

        assertThat(logout.statusCode()).isEqualTo(200);
        assertThat(logout.body()).isEmpty();
        assertThat(logout.headers().allValues("Set-Cookie"))
                .anySatisfy(cookie -> assertThat(cookie).contains("SESSION=", "Max-Age=0"));
        assertThat(first.get(PROBE).statusCode()).isEqualTo(401);
        assertThat(second.get(PROBE).statusCode()).isEqualTo(200);
        assertThat(sessions.findById(firstId)).isNull();
        staleCopy.setLastAccessedTime(Instant.now());
        staleCopy.setAttribute(CONTEXT, staleCopy.getAttribute(CONTEXT));
        sessions.save(staleCopy);
        assertThat(sessions.findById(firstId)).isNull();
        assertThat(sessions.findById(secondId)).isNotNull();
    }

    @Test
    @Order(2)
    void browserCookieHasNoExpiryAndOnlyTheThirtyDayServerIdlePeriodRollsForward() throws Exception {
        AuthenticatedUser user = account(21L, "rolling-user@example.test", "ROLE_USER");
        Browser browser = new Browser();
        HttpResponse<String> login = login(browser, user);
        String id = sessionId(browser);
        RedisSession before = stored(id);
        assertThat(before.getMaxInactiveInterval()).isEqualTo(THIRTY_DAYS);
        assertBrowserSessionCookie(login);
        Instant oldAccess = Instant.now().minus(Duration.ofDays(29));
        before.setLastAccessedTime(oldAccess);
        sessions.save(before);
        Instant requestStarted = Instant.now().minusSeconds(1);

        HttpResponse<String> probe = browser.get(PROBE);

        assertThat(probe.statusCode()).isEqualTo(200);
        RedisSession refreshed = stored(id);
        assertThat(refreshed.getMaxInactiveInterval()).isEqualTo(THIRTY_DAYS);
        assertThat(refreshed.getLastAccessedTime()).isAfterOrEqualTo(requestStarted).isAfter(oldAccess);
        assertThat(sessionId(browser)).isEqualTo(id);
        assertThat(probe.headers().allValues("Set-Cookie").size()).isZero();
    }

    @Test
    @Order(3)
    void expiryRequiresReloginRotatesSessionIdAndRejectsThePreviousCsrfToken() throws Exception {
        AuthenticatedUser user = account(31L, "expired-user@example.test", "ROLE_USER");
        Browser browser = new Browser();
        csrf(browser);
        String anonymousId = sessionId(browser);
        login(browser, user);
        String oldId = sessionId(browser);
        assertThat(oldId).isNotEqualTo(anonymousId);
        String oldCsrf = csrf(browser);
        RedisSession expired = stored(oldId);
        expired.setLastAccessedTime(Instant.now().minus(Duration.ofDays(31)));
        sessions.save(expired);
        assertThat(sessions.findById(oldId)).isNull();
        assertThat(browser.get(PROBE).statusCode()).isEqualTo(401);

        login(browser, user);

        assertThat(sessionId(browser)).isNotEqualTo(oldId);
        assertThat(browser.post(MUTATION, oldCsrf).statusCode()).isEqualTo(403);
        assertThat(browser.post(MUTATION, csrf(browser)).statusCode()).isEqualTo(204);
        assertThat(browser.get(PROBE).statusCode()).isEqualTo(200);
    }

    @Test
    @Order(4)
    void withdrawingAnAccountInvalidatesAllItsSessionsButPreservesANewAccountWithTheSameEmail() throws Exception {
        String sharedEmail = "reused-email@example.test";
        AuthenticatedUser oldUser = account(41L, sharedEmail, "ROLE_USER");
        AuthenticatedUser newUser = account(42L, sharedEmail, "ROLE_USER");
        Browser firstOldDevice = new Browser();
        Browser secondOldDevice = new Browser();
        Browser newAccountDevice = new Browser();
        login(firstOldDevice, oldUser);
        login(secondOldDevice, oldUser);
        login(newAccountDevice, newUser);
        String firstOldId = sessionId(firstOldDevice);
        String secondOldId = sessionId(secondOldDevice);
        String newId = sessionId(newAccountDevice);
        assertThat(sessions.findByPrincipalName(sharedEmail)).containsKeys(firstOldId, secondOldId, newId);

        invalidator.invalidateAll(oldUser.id(), sharedEmail);
        accounts.remove(oldUser.id());

        assertThat(sessions.findById(firstOldId)).isNull();
        assertThat(sessions.findById(secondOldId)).isNull();
        assertThat(sessions.findById(newId)).isNotNull();
        assertThat(firstOldDevice.get(PROBE).statusCode()).isEqualTo(401);
        assertThat(secondOldDevice.get(PROBE).statusCode()).isEqualTo(401);
        assertThat(newAccountDevice.get(PROBE).statusCode()).isEqualTo(200);
    }

    @Test
    @Order(5)
    void currentDatabaseRoleAndDeletedAccountLookupAreRecheckedForEveryRequest() throws Exception {
        AuthenticatedUser user = account(51L, "role-user@example.test", "ROLE_ADMIN");
        Browser browser = new Browser();
        login(browser, user);
        String id = sessionId(browser);
        assertThat(browser.get(ADMIN).statusCode()).isEqualTo(200);
        account(user.id(), user.email(), "ROLE_USER");

        assertThat(browser.get(ADMIN).statusCode()).isEqualTo(403);
        assertThat(browser.get(PROBE).statusCode()).isEqualTo(200);
        accounts.remove(user.id());
        assertThat(browser.get(PROBE).statusCode()).isEqualTo(401);
        assertThat(sessions.findById(id)).isNull();
    }

    @Test
    @Order(6)
    void databaseLookupFailureRejectsAccessWithoutDeletingTheStillValidSession() throws Exception {
        AuthenticatedUser user = account(61L, "lookup-failure@example.test", "ROLE_USER");
        Browser browser = new Browser();
        login(browser, user);
        String id = sessionId(browser);
        when(users.findAuthenticatedUser(user.id(), user.email())).thenThrow(new IllegalStateException("test lookup"));

        HttpResponse<String> response = browser.get(PROBE);

        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(objectMapper.readTree(response.body()).path("code").asText()).isEqualTo("G001");
        assertThat(sessions.findById(id)).isNotNull();
        assertThat(sessionId(browser)).isEqualTo(id);
    }

    @Test
    @Order(99)
    void redisOutageRejectsAnExistingSessionInsteadOfUsingCachedAuthentication() throws Exception {
        AuthenticatedUser user = account(99L, "outage-user@example.test", "ROLE_USER");
        Browser browser = new Browser();
        login(browser, user);
        assertThat(browser.get(PROBE).statusCode()).isEqualTo(200);
        String cookieBeforeOutage = browser.sessionCookie();

        REDIS.stop();
        HttpResponse<String> response = browser.get(PROBE);

        assertThat(response.statusCode()).isBetween(400, 599);
        assertThat(browser.sessionCookie()).isEqualTo(cookieBeforeOutage);
    }

    private AuthenticatedUser account(Long id, String email, String role) {
        AuthenticatedUser user = new AuthenticatedUser(id, email, role);
        accounts.put(id, user);
        return user;
    }

    private HttpResponse<String> login(Browser browser, AuthenticatedUser user) throws Exception {
        HttpResponse<String> response = browser.get("/oauth2/session-e2e-login?userId=" + user.id()
                + "&email=" + URLEncoder.encode(user.email(), StandardCharsets.UTF_8));
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("Location")).contains("http://localhost:3000");
        assertThat(browser.sessionCookie()).isNotBlank();
        return response;
    }

    private String csrf(Browser browser) throws Exception {
        HttpResponse<String> response = browser.get(CSRF);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        String token = objectMapper.readTree(response.body()).path("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private String sessionId(Browser browser) {
        return new String(Base64.getDecoder().decode(browser.sessionCookie()), StandardCharsets.UTF_8);
    }

    private RedisSession stored(String id) {
        RedisSession session = sessions.findById(id);
        assertThat(session).isNotNull();
        return session;
    }

    private void assertBrowserSessionCookie(HttpResponse<String> response) {
        var headers = response.headers().allValues("Set-Cookie").stream()
                .filter(header -> header.startsWith("SESSION=")).toList();
        assertThat(headers.size()).isEqualTo(1);
        String cookie = headers.getFirst();
        assertThat(cookie.contains("Max-Age=")).as("browser session cookie has no Max-Age").isFalse();
        assertThat(cookie.contains("Expires=")).as("browser session cookie has no Expires").isFalse();
        assertThat(cookie.contains("HttpOnly")).isTrue();
        assertThat(cookie.contains("SameSite=Lax")).isTrue();
    }

    private final class Browser {
        private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        private final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies)
                .connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();

        HttpResponse<String> get(String path) throws IOException, InterruptedException {
            return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                    .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
        }

        HttpResponse<String> post(String path, String csrf) throws IOException, InterruptedException {
            return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                    .timeout(Duration.ofSeconds(10)).header("X-CSRF-TOKEN", csrf)
                    .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        }

        String sessionCookie() {
            return cookies.getCookieStore().getCookies().stream().filter(cookie -> "SESSION".equals(cookie.getName()))
                    .map(HttpCookie::getValue).findFirst().orElse("");
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class,
            FlywayAutoConfiguration.class, RedisRepositoriesAutoConfiguration.class
    })
    @Import({RedisConfig.class, SecurityContextRepositoryConfig.class, SecurityConfig.class,
            SessionAuthenticationFilter.class, CustomAccessDeniedHandler.class, CustomAuthenticationEntryPoint.class,
            AuthService.class, AuthController.class, SessionInvalidator.class, OAuth2SuccessHandler.class,
            OAuth2FailureHandler.class, GlobalExceptionHandler.class, SessionFixtureController.class})
    static class TestApplication {
    }

    @RestController
    static class SessionFixtureController {
        private final OAuth2SuccessHandler successHandler;

        SessionFixtureController(OAuth2SuccessHandler successHandler) {
            this.successHandler = successHandler;
        }

        @GetMapping("/oauth2/session-e2e-login")
        void login(@RequestParam Long userId, @RequestParam String email, HttpServletRequest request,
                   HttpServletResponse response) throws IOException {
            OAuth2AuthenticatedUser principal = new OAuth2AuthenticatedUser(userId, email, "ROLE_USER",
                    Map.of("sub", "test-" + userId, "email", email), "sub");
            successHandler.onAuthenticationSuccess(request, response,
                    new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google"));
        }

        @GetMapping(PROBE)
        Map<String, String> probe() {
            SecurityContext context = SecurityContextHolder.getContext();
            return Map.of("name", context.getAuthentication().getName());
        }

        @PostMapping(MUTATION)
        ResponseEntity<Void> mutate() {
            return ResponseEntity.noContent().build();
        }

        @GetMapping(ADMIN)
        ResponseEntity<Void> admin() {
            return ResponseEntity.ok().build();
        }
    }
}
