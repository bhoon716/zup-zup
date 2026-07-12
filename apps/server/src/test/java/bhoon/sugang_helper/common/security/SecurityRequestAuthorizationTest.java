package bhoon.sugang_helper.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bhoon.sugang_helper.admin.presentation.AdminController;
import bhoon.sugang_helper.admin.application.AdminService;
import bhoon.sugang_helper.announcement.presentation.AnnouncementController;
import bhoon.sugang_helper.announcement.application.AnnouncementSearchType;
import bhoon.sugang_helper.announcement.application.AnnouncementService;
import bhoon.sugang_helper.common.security.exception.CustomAccessDeniedHandler;
import bhoon.sugang_helper.common.security.exception.CustomAuthenticationEntryPoint;
import bhoon.sugang_helper.common.security.jwt.JwtAuthenticationFilter;
import bhoon.sugang_helper.common.security.jwt.JwtProvider;
import bhoon.sugang_helper.common.security.oauth.CustomOAuth2UserService;
import bhoon.sugang_helper.common.security.oauth.OAuth2FailureHandler;
import bhoon.sugang_helper.common.security.oauth.OAuth2SuccessHandler;
import bhoon.sugang_helper.common.config.SecurityConfig;
import bhoon.sugang_helper.feedback.application.FeedbackService;
import bhoon.sugang_helper.feedback.presentation.FeedbackController;
import bhoon.sugang_helper.user.application.UserService;
import bhoon.sugang_helper.user.domain.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = SecurityRequestAuthorizationTest.TestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.cors.allowed-origins=https://zup-zup.com",
        "app.cors.require-https=true",
        "management.health.mail.enabled=false",
        "app.oauth2.success-redirect-uri=http://localhost:3000",
        "app.oauth2.failure-redirect-uri=http://localhost:3000/login?error=true",
        "app.oauth2.authorized-redirect-uri=http://localhost:3000",
        "spring.security.oauth2.client.registration.google.client-id=test-client",
        "spring.security.oauth2.client.registration.google.client-secret=test-secret",
        "spring.security.oauth2.client.registration.google.scope=profile,email",
        "spring.security.oauth2.client.registration.google.authorization-grant-type=authorization_code",
        "spring.security.oauth2.client.provider.google.authorization-uri=https://accounts.google.com/o/oauth2/v2/auth",
        "spring.security.oauth2.client.provider.google.token-uri=https://oauth2.googleapis.com/token",
        "spring.security.oauth2.client.provider.google.user-info-uri=https://www.googleapis.com/oauth2/v3/userinfo",
        "spring.security.oauth2.client.provider.google.user-name-attribute=sub"
})
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // Authorization cases intentionally exercise one protected path.
class SecurityRequestAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private JwtProvider jwtProvider;
    @MockitoBean
    private AdminService adminService;
    @MockitoBean
    private AnnouncementService announcementService;
    @MockitoBean
    private FeedbackService feedbackService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean
    private OAuth2SuccessHandler oAuth2SuccessHandler;
    @MockitoBean
    private OAuth2FailureHandler oAuth2FailureHandler;

    @Test
    void adminDashboardWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A001"))
                .andExpect(jsonPath("$.path").value("/api/v1/admin/dashboard"));
    }

    @Test
    void adminDashboardWithNonAdminUserIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard").session(authenticatedSession("ROLE_USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("A002"))
                .andExpect(jsonPath("$.path").value("/api/v1/admin/dashboard"));
    }

    @Test
    void publicAnnouncementListIsAccessibleWithoutAuthentication() throws Exception {
        given(announcementService.getPublicAnnouncements(null, AnnouncementSearchType.TITLE_CONTENT))
                .willReturn(List.of());

        mockMvc.perform(get("/api/v1/announcements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("공지사항 목록입니다."));
    }

    @Test
    void uploadPathWithoutAuthenticationIsRejectedBeforeStaticResourceHandling() throws Exception {
        mockMvc.perform(get("/uploads/private-feedback.png"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A001"));
    }

    @Test
    void attachmentDownloadWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/feedbacks/10/attachments/20"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A001"));
    }

    @Test
    void actuatorInfoWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A001"));
    }

    @Test
    void internalManagementHealthAndMetricsRemainAvailable() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .isNotIn(HttpStatus.UNAUTHORIZED.value(), HttpStatus.FORBIDDEN.value()));
    }

    @Test
    void developerToolsRequireAdminRole() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A001"));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A001"));

        mockMvc.perform(get("/h2-console/"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A001"));
    }

    @Test
    void adminCanAccessDeveloperToolsOutsideProduction() throws Exception {
        mockMvc.perform(get("/v3/api-docs").session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void sessionSecurityContextAuthenticatesProtectedRequestWithoutStoringJwt() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard").session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void expiredSessionSecurityContextIsRejectedUntilRefreshesAgain() throws Exception {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("admin@example.com", "",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        session.setAttribute("AUTHENTICATION_EXPIRES_AT", 0L);

        mockMvc.perform(get("/api/v1/admin/dashboard").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deletedAccountSessionIsRejectedBeforeTheOriginalExpiry() throws Exception {
        MockHttpSession session = authenticatedSession("ROLE_ADMIN");
        given(userRepository.existsByIdAndEmailAndDeletedAtIsNull(1L, "admin@example.com")).willReturn(false);

        mockMvc.perform(get("/api/v1/admin/dashboard").session(session))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpSession authenticatedSession(String role) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("admin@example.com", "",
                List.of(new SimpleGrantedAuthority(role))));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        session.setAttribute("AUTHENTICATION_EXPIRES_AT", System.currentTimeMillis() + 60_000L);
        session.setAttribute("AUTHENTICATION_USER_ID", 1L);
        given(userRepository.existsByIdAndEmailAndDeletedAtIsNull(1L, "admin@example.com")).willReturn(true);
        return session;
    }

    @Test
    void corsAllowsOnlyTheExplicitOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/announcements")
                        .header(HttpHeaders.ORIGIN, "https://zup-zup.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://zup-zup.com"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));

        mockMvc.perform(options("/api/v1/announcements")
                        .header(HttpHeaders.ORIGIN, "https://untrusted.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            JpaRepositoriesAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            RedisAutoConfiguration.class,
            RedisRepositoriesAutoConfiguration.class,
            SessionAutoConfiguration.class
    })
    @Import({
            SecurityConfig.class,
            JwtAuthenticationFilter.class,
            CustomAccessDeniedHandler.class,
            CustomAuthenticationEntryPoint.class,
            AdminController.class,
            AnnouncementController.class,
            FeedbackController.class
    })
    static class TestApplication {
    }
}
