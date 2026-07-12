package bhoon.sugang_helper.common.security;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = SecurityRequestAuthorizationTest.TestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:3000",
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
    @WithMockUser(roles = "USER")
    void adminDashboardWithNonAdminUserIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
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
