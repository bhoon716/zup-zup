package bhoon.sugang_helper.common.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bhoon.sugang_helper.admin.audit.controller.AdminActionLogController;
import bhoon.sugang_helper.admin.audit.service.AdminAuditService;
import bhoon.sugang_helper.admin.controller.AdminController;
import bhoon.sugang_helper.admin.service.AdminService;
import bhoon.sugang_helper.campus.announcement.controller.AnnouncementController;
import bhoon.sugang_helper.campus.announcement.enums.AnnouncementSearchType;
import bhoon.sugang_helper.campus.announcement.service.AnnouncementService;
import bhoon.sugang_helper.identity.auth.controller.AuthController;
import bhoon.sugang_helper.identity.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.test.web.servlet.MvcResult;
import bhoon.sugang_helper.common.config.SecurityConfig;
import bhoon.sugang_helper.common.config.SecurityContextRepositoryConfig;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.security.exception.CustomAccessDeniedHandler;
import bhoon.sugang_helper.common.security.exception.CustomAuthenticationEntryPoint;
import bhoon.sugang_helper.common.security.session.SessionAuthenticationFilter;
import bhoon.sugang_helper.common.web.GlobalExceptionHandler;
import bhoon.sugang_helper.identity.auth.client.CustomOAuth2UserService;
import bhoon.sugang_helper.identity.auth.handler.OAuth2FailureHandler;
import bhoon.sugang_helper.identity.auth.handler.OAuth2SuccessHandler;
import bhoon.sugang_helper.identity.user.service.UserIdentityQueryService;
import bhoon.sugang_helper.seatalert.notification.controller.AdminNotificationDeliveryController;
import bhoon.sugang_helper.seatalert.notification.service.AdminNotificationDeliveryService;
import bhoon.sugang_helper.support.feedback.controller.AdminFeedbackController;
import bhoon.sugang_helper.support.feedback.controller.FeedbackController;
import bhoon.sugang_helper.support.feedback.dto.result.FeedbackAttachmentDownload;
import bhoon.sugang_helper.support.feedback.service.FeedbackService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
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
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import bhoon.sugang_helper.identity.auth.service.SessionPrincipal;
import bhoon.sugang_helper.identity.user.dto.result.AuthenticatedUser;
import org.springframework.session.web.http.CookieSerializer;
import java.util.Optional;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.unit.DataSize;

@SpringBootTest(classes = SecurityRequestAuthorizationTest.TestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.cors.allowed-origins=https://zup-zup.com",
        "app.cors.require-https=true",
        "management.health.mail.enabled=false",
        "management.endpoint.health.probes.enabled=true",
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
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class SecurityRequestAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CsrfTokenRepository csrfTokenRepository;
    @MockitoBean private AuthService authService;
    @Autowired
    private MultipartProperties multipartProperties;
    @MockitoBean
    private CookieSerializer cookieSerializer;
    @MockitoBean
    private AdminService adminService;
    @MockitoBean
    private AdminAuditService adminAuditService;
    @MockitoBean
    private AdminNotificationDeliveryService adminNotificationDeliveryService;
    @MockitoBean
    private AnnouncementService announcementService;
    @MockitoBean
    private FeedbackService feedbackService;
    @MockitoBean
    private UserIdentityQueryService userIdentityQuery;
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
    void adminAuditLogsWithoutAuthenticationAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A001"));
    }

    @Test
    void adminAuditLogsWithNonAdminUserAreForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs").session(authenticatedSession("ROLE_USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("A002"));
    }

    @Test
    void adminCanAccessAllAuditLogs() throws Exception {
        given(adminAuditService.getActionLogs(any()))
                .willReturn(Page.empty());

        mockMvc.perform(get("/api/v1/admin/audit-logs?page=1&size=2").session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void notificationDeliveryOperationsRequireAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/notification-deliveries/dlq"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A001"));

        mockMvc.perform(get("/api/v1/admin/notification-deliveries/dlq")
                        .session(authenticatedSession("ROLE_USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("A002"));

        given(adminNotificationDeliveryService.getDeadLetters(any()))
                .willReturn(Page.empty());
        mockMvc.perform(get("/api/v1/admin/notification-deliveries/dlq")
                        .session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(post("/api/v1/admin/notification-deliveries/1/replay").with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A001"));

        mockMvc.perform(post("/api/v1/admin/notification-deliveries/1/replay").with(csrf().asHeader())
                        .session(authenticatedSession("ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("A002"));
    }

    @Test
    void adminFeedbackListRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/feedbacks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A001"));

        mockMvc.perform(get("/api/v1/admin/feedbacks").session(authenticatedSession("ROLE_USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("A002"));

        given(feedbackService.getFeedbacksForAdmin(
                any(), any()))
                .willReturn(Page.empty());
        mockMvc.perform(get("/api/v1/admin/feedbacks?deletion=DELETED")
                        .session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void adminFeedbackAttachmentDownloadRequiresAdminRole() throws Exception {
        mockMvc.perform(post("/api/v1/admin/feedbacks/10/attachments/20/download").with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmed\":true}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A001"));

        mockMvc.perform(post("/api/v1/admin/feedbacks/10/attachments/20/download").with(csrf().asHeader())
                        .session(authenticatedSession("ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmed\":true}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("A002"));
    }

    @Test
    void adminCanPreviewSanitizedAttachmentWithItsActualContentType() throws Exception {
        given(feedbackService.getAttachmentForCurrentAdmin(10L, 20L, true)).willReturn(
                new FeedbackAttachmentDownload(new ByteArrayResource(new byte[] {1, 2, 3}),
                        "attachment.png", MediaType.IMAGE_PNG));

        mockMvc.perform(post("/api/v1/admin/feedbacks/10/attachments/20/download").with(csrf().asHeader())
                        .session(authenticatedSession("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmed\":true}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("attachment.png")));
    }

    @Test
    void adminAttachmentRequestWithMismatchedFeedbackAndAttachmentReturnsNotFound() throws Exception {
        given(feedbackService.getAttachmentForCurrentAdmin(10L, 99L, true))
                .willThrow(new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));

        mockMvc.perform(post("/api/v1/admin/feedbacks/10/attachments/99/download").with(csrf().asHeader())
                        .session(authenticatedSession("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmed\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("F001"));
    }

    @Test
    void publicAnnouncementListIsAccessibleWithoutAuthentication() throws Exception {
        given(announcementService.getPublicAnnouncements(eq(null), eq(AnnouncementSearchType.TITLE_CONTENT), any()))
                .willReturn(Page.empty());

        mockMvc.perform(get("/api/v1/announcements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.code").doesNotExist());
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
    void multipartIngressBudgetMatchesFeedbackAttachmentPolicy() {
        assertThat(multipartProperties.getMaxFileSize()).isEqualTo(DataSize.ofMegabytes(10));
        assertThat(multipartProperties.getMaxRequestSize()).isEqualTo(DataSize.ofMegabytes(11));
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

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .isNotIn(HttpStatus.UNAUTHORIZED.value(), HttpStatus.FORBIDDEN.value()));
    }

    @Test
    void h2ConsoleRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/h2-console/"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A001"));
    }

    @Test
    void sessionSecurityContextAuthenticatesProtectedRequestWithIdentity() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard").session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void legacySessionPrincipalCannotAuthenticate() throws Exception {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("legacy@example.com", "",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        mockMvc.perform(get("/api/v1/admin/dashboard").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deletedAccountSessionIsRejectedBeforeTheOriginalExpiry() throws Exception {
        MockHttpSession session = authenticatedSession("ROLE_ADMIN");
        given(userIdentityQuery.findAuthenticatedUser(1L, "admin@example.com")).willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/admin/dashboard").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void csrfEndpointReturnsAMaskedTokenAndMutationFailuresUseTheirOwnCode() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isString()).andReturn();
        MockHttpSession session = (MockHttpSession) csrf.getRequest().getSession(false);
        String token = objectMapper.readTree(csrf.getResponse().getContentAsString()).path("token").asText();
        String rawToken = csrfTokenRepository.loadToken(csrf.getRequest()).getToken();
        assertThat(token).isNotBlank().isNotEqualTo(rawToken);
        assertThat(csrf.getResponse().getHeader(HttpHeaders.CACHE_CONTROL)).contains("no-store");

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("A003"));
        mockMvc.perform(post("/api/auth/logout").session(session).header("X-CSRF-TOKEN", rawToken))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("A003"));
        mockMvc.perform(post("/api/auth/logout").session(session).header("X-CSRF-TOKEN", token))
                .andExpect(status().isOk());
    }

    @Test
    void csrfExceptionIsRestrictedToTheExactReadOnlySearchPost() throws Exception {
        mockMvc.perform(post("/api/v1/courses/search"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/courses/search/"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("A003"));
        mockMvc.perform(post("/api/v1/courses/1/reviews"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("A003"));
    }

    private MockHttpSession authenticatedSession(String role) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new SessionPrincipal(1L, "admin@example.com").authentication(role));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        when(userIdentityQuery.findAuthenticatedUser(1L, "admin@example.com"))
                .thenReturn(Optional.of(new AuthenticatedUser(1L, "admin@example.com", role)));
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
            AuthController.class,
            SecurityConfig.class,
            SecurityContextRepositoryConfig.class,
            SessionAuthenticationFilter.class,
            CustomAccessDeniedHandler.class,
            CustomAuthenticationEntryPoint.class,
            AdminController.class,
            AdminActionLogController.class,
            AdminNotificationDeliveryController.class,
            AdminFeedbackController.class,
            AnnouncementController.class,
            FeedbackController.class,
            GlobalExceptionHandler.class
    })
    static class TestApplication {
    }
}
