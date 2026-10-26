package bhoon.sugang_helper.common.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bhoon.sugang_helper.common.config.SecurityConfig;
import bhoon.sugang_helper.common.config.SecurityContextRepositoryConfig;
import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.common.security.exception.CustomAccessDeniedHandler;
import bhoon.sugang_helper.common.security.exception.CustomAuthenticationEntryPoint;
import bhoon.sugang_helper.common.web.GlobalExceptionHandler;
import bhoon.sugang_helper.identity.auth.client.CustomOAuth2UserService;
import bhoon.sugang_helper.identity.auth.controller.AuthController;
import bhoon.sugang_helper.identity.auth.handler.OAuth2FailureHandler;
import bhoon.sugang_helper.identity.auth.handler.OAuth2SuccessHandler;
import bhoon.sugang_helper.identity.auth.service.AuthService;
import bhoon.sugang_helper.identity.auth.service.OAuth2AuthenticatedUser;
import bhoon.sugang_helper.identity.user.dto.result.AuthenticatedUser;
import bhoon.sugang_helper.identity.user.service.UserIdentityQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(classes = AuthenticationFlowTest.TestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "jwt.secret=authenticationFlowTestKeyauthenticationFlowTestKey",
        "jwt.access.expiration=1800000",
        "jwt.refresh.expiration=604800000",
        "app.cors.allowed-origins=https://web.example.com",
        "app.oauth2.success-redirect-uri=https://web.example.com",
        "spring.security.oauth2.client.registration.google.client-id=test-client",
        "spring.security.oauth2.client.registration.google.client-secret=test-secret",
        "spring.security.oauth2.client.registration.google.scope=profile,email",
        "spring.security.oauth2.client.registration.google.authorization-grant-type=authorization_code",
        "spring.security.oauth2.client.provider.google.authorization-uri=https://accounts.example.com/auth",
        "spring.security.oauth2.client.provider.google.token-uri=https://accounts.example.com/token",
        "spring.security.oauth2.client.provider.google.user-info-uri=https://accounts.example.com/userinfo",
        "spring.security.oauth2.client.provider.google.user-name-attribute=sub"
})
class AuthenticationFlowTest {

    private static final String EMAIL = "user@example.com";
    private static final String USER_ROLE = "ROLE_USER";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ME_PATH = "/api/v1/auth-test/me";
    private static final String ADMIN_PATH = "/api/v1/admin/auth-test";
    private static final String TEST_KEY = "authenticationFlowTestKeyauthenticationFlowTestKey";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ServletContext servletContext;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private OAuth2SuccessHandler successHandler;
    @Autowired
    private InMemoryRedisService redisService;
    @MockitoBean
    private UserIdentityQueryService userIdentityQuery;
    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean
    private OAuth2FailureHandler failureHandler;

    @BeforeEach
    void setUp() {
        redisService.clear();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginSessionExpiryRefreshAuthorizationAndLogoutKeepTheirHttpContracts() throws Exception {
        MockHttpServletRequest loginRequest = new MockHttpServletRequest(servletContext);
        MockHttpServletResponse loginResponse = new MockHttpServletResponse();
        var principal = new OAuth2AuthenticatedUser(1L, EMAIL, USER_ROLE, Map.of("sub", "test-subject"), "sub");
        Authentication oauth = new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities());
        given(userIdentityQuery.findAuthenticatedUser(1L, EMAIL))
                .willReturn(Optional.of(new AuthenticatedUser(1L, EMAIL, USER_ROLE)));

        successHandler.onAuthenticationSuccess(loginRequest, loginResponse, oauth);
        MockHttpSession loginSession = (MockHttpSession) loginRequest.getSession(false);
        Cookie refreshCookie = refreshCookie(loginResponse);
        String initialRecord = redisService.getValues("RT:" + EMAIL);
        assertThat(loginResponse.getRedirectedUrl()).isEqualTo("https://web.example.com");
        assertThat(loginSession).isNotNull();
        assertThat(loginSession.getAttribute("ACCESS_TOKEN")).isNull();
        assertThat(loginSession.getAttribute("REFRESH_TOKEN")).isNull();
        mockMvc.perform(get(ME_PATH).session(loginSession))
                .andExpect(status().isOk()).andExpect(content().string(EMAIL));
        loginSession.setAttribute("AUTHENTICATION_EXPIRES_AT", 0L);
        mockMvc.perform(get(ME_PATH).session(loginSession)).andExpect(status().isUnauthorized());

        MvcResult refresh = mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isString()).andReturn();
        String accessToken = objectMapper.readValue(refresh.getResponse().getContentAsString(), String.class);
        Cookie rotatedRefreshCookie = refreshCookie(refresh.getResponse());
        MockHttpSession renewedSession = (MockHttpSession) refresh.getRequest().getSession(false);
        assertThat(rotatedRefreshCookie.getValue()).isNotEqualTo(refreshCookie.getValue());
        assertThat(redisService.getValues("RT:" + EMAIL)).isNotEqualTo(initialRecord);
        assertThat(redisService.duration("RT:" + EMAIL)).isEqualTo(Duration.ofDays(7));
        mockMvc.perform(get(ME_PATH).session(renewedSession)).andExpect(status().isOk());
        mockMvc.perform(get(ADMIN_PATH).session(renewedSession)).andExpect(status().isForbidden());

        MvcResult logout = mockMvc.perform(post("/api/auth/logout").session(renewedSession)
                        .cookie(rotatedRefreshCookie).header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + accessToken))
                .andExpect(status().isOk()).andExpect(content().string("")).andReturn();
        assertThat(logout.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                .hasSize(2).allMatch(value -> value.contains("Max-Age=0"));
        assertThat(renewedSession.isInvalid()).isTrue();
        assertThat(redisService.getValues("RT:" + EMAIL)).isNull();
        mockMvc.perform(get(ME_PATH)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(ME_PATH).header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + accessToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/refresh").cookie(rotatedRefreshCookie))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(customOAuth2UserService, failureHandler);
    }

    @ParameterizedTest
    @MethodSource("invalidBearerTokens")
    void invalidBearerNeverAuthenticatesProtectedRequests(String token) throws Exception {
        mockMvc.perform(get(ME_PATH).header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bearerUsesSignedRoleAndUserRevocationMarkers() throws Exception {
        String userToken = jwtProvider.createAccessToken(1L, EMAIL, USER_ROLE);
        String adminToken = jwtProvider.createAccessToken(1L, EMAIL, "ROLE_ADMIN");

        mockMvc.perform(get(ADMIN_PATH).header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + userToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(ADMIN_PATH).header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + adminToken))
                .andExpect(status().isOk());
        redisService.setValues("UA:1", "logout", Duration.ofMinutes(30));
        mockMvc.perform(get(ADMIN_PATH).header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + adminToken))
                .andExpect(status().isUnauthorized());
    }

    private Cookie refreshCookie(MockHttpServletResponse response) {
        String header = response.getHeaders(HttpHeaders.SET_COOKIE).stream()
                .filter(value -> value.startsWith("refresh_token="))
                .findFirst().orElseThrow();
        return new Cookie("refresh_token", header.substring("refresh_token=".length(), header.indexOf(';')));
    }

    private static Stream<String> invalidBearerTokens() {
        return Stream.of("not-a-jwt",
                signedToken("refresh", 1L, USER_ROLE, 60_000L, TEST_KEY),
                signedToken("access", 1L, USER_ROLE, -1_000L, TEST_KEY),
                signedToken("access", null, USER_ROLE, 60_000L, TEST_KEY),
                signedToken("access", 1L, "ROLE_ADMIN", 60_000L, "differentSigningKeydifferentSigningKey"));
    }

    private static String signedToken(String type, Long userId, String role, long lifetime, String key) {
        return Jwts.builder().subject(EMAIL).claim("token_type", type).claim("uid", userId).claim("role", role)
                .expiration(new Date(System.currentTimeMillis() + lifetime))
                .signWith(Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8))).compact();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
            JpaRepositoriesAutoConfiguration.class, RedisAutoConfiguration.class, RedisRepositoriesAutoConfiguration.class,
            FlywayAutoConfiguration.class, SessionAutoConfiguration.class})
    @Import({SecurityConfig.class, SecurityContextRepositoryConfig.class, JwtProvider.class,
            JwtAuthenticationFilter.class, UserAccessRevocationService.class, AuthService.class, AuthController.class,
            OAuth2SuccessHandler.class, CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class,
            GlobalExceptionHandler.class, ProbeController.class})
    static class TestApplication {
        @Bean
        InMemoryRedisService redisService() {
            return new InMemoryRedisService();
        }
    }

    @RestController
    static class ProbeController {
        @GetMapping({ME_PATH, ADMIN_PATH})
        String currentUser(Authentication authentication) {
            return authentication.getName();
        }
    }

    static final class InMemoryRedisService extends RedisService {
        private final Map<String, String> values = new HashMap<>();
        private final Map<String, Duration> durations = new HashMap<>();

        private InMemoryRedisService() {
            super(null);
        }

        @Override
        public void setValues(String key, String value, Duration duration) {
            values.put(key, value);
            durations.put(key, duration);
        }

        @Override
        public String getValues(String key) {
            return values.get(key);
        }

        @Override
        public boolean hasKey(String key) {
            return values.containsKey(key);
        }

        @Override
        public boolean compareAndSetValues(String key, String expected, String replacement, Duration duration) {
            if (!expected.equals(values.get(key))) {
                return false;
            }
            setValues(key, replacement, duration);
            return true;
        }

        @Override
        public boolean compareAndDeleteValues(String key, String expected) {
            return values.remove(key, expected);
        }

        private Duration duration(String key) {
            return durations.get(key);
        }

        private void clear() {
            values.clear();
            durations.clear();
        }
    }
}
