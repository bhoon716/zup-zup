package bhoon.sugang_helper.common.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bhoon.sugang_helper.campus.schedule.controller.AdminScheduleController;
import bhoon.sugang_helper.campus.schedule.controller.ScheduleController;
import bhoon.sugang_helper.campus.schedule.dto.response.ScheduleResponse;
import bhoon.sugang_helper.campus.schedule.entity.Schedule;
import bhoon.sugang_helper.campus.schedule.service.ScheduleService;
import bhoon.sugang_helper.common.config.SecurityConfig;
import bhoon.sugang_helper.common.config.SecurityContextRepositoryConfig;
import bhoon.sugang_helper.common.security.exception.CustomAccessDeniedHandler;
import bhoon.sugang_helper.common.security.exception.CustomAuthenticationEntryPoint;
import bhoon.sugang_helper.common.security.session.SessionAuthenticationFilter;
import bhoon.sugang_helper.common.web.GlobalExceptionHandler;
import bhoon.sugang_helper.identity.auth.client.CustomOAuth2UserService;
import bhoon.sugang_helper.identity.auth.handler.OAuth2FailureHandler;
import bhoon.sugang_helper.identity.auth.handler.OAuth2SuccessHandler;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import bhoon.sugang_helper.identity.user.service.UserIdentityQueryService;
import bhoon.sugang_helper.identity.auth.service.SessionPrincipal;
import bhoon.sugang_helper.identity.user.dto.result.AuthenticatedUser;
import org.springframework.session.web.http.CookieSerializer;
import java.util.Optional;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = ScheduleAuthorizationTest.TestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.cors.allowed-origins=https://zup-zup.com",
        "app.oauth2.success-redirect-uri=http://localhost:3000",
        "app.oauth2.failure-redirect-uri=http://localhost:3000/login?error=true",
        "app.oauth2.authorized-redirect-uri=http://localhost:3000",
        "management.health.mail.enabled=false",
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
class ScheduleAuthorizationTest {

    private static final String ADMIN_PATH = "/api/v1/admin/schedules";
    private static final String REQUEST = """
            {"scheduleType":"수강신청","startDate":"2026-10-26","endDate":"2026-10-27",
             "startTime":"09:00","endTime":"18:00","ddayEnabled":true}
            """;
    @Autowired private MockMvc mockMvc;
    @MockitoBean private ScheduleService schedules;
    @MockitoBean private CookieSerializer cookieSerializer;
    @MockitoBean private UserIdentityQueryService userIdentityQuery;
    @MockitoBean private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean private OAuth2SuccessHandler oAuth2SuccessHandler;
    @MockitoBean private OAuth2FailureHandler oAuth2FailureHandler;

    @Test
    void schedulesAndDdaySelectionArePublicRawArrays() throws Exception {
        when(schedules.getUpcomingSchedules()).thenReturn(List.of(response()));
        when(schedules.getDdaySchedules()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/v1/schedules"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].scheduleType").value("수강신청"))
                .andExpect(jsonPath("$[0].ddayEnabled").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
        mockMvc.perform(get("/api/v1/schedules/ddays"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].dDay").value("D-Day"));
        when(schedules.getDdaySchedules()).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/schedules/ddays"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PUT", "DELETE", "PATCH"})
    void everyAdminOperationRequiresAnAdmin(String method) throws Exception {
        String path = switch (method) {
            case "PUT", "DELETE" -> ADMIN_PATH + "/1";
            case "PATCH" -> ADMIN_PATH + "/1/restore";
            default -> ADMIN_PATH;
        };
        mockMvc.perform(request(HttpMethod.valueOf(method), path).with(csrf().asHeader()).contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("A001"));
        mockMvc.perform(request(HttpMethod.valueOf(method), path).with(csrf().asHeader()).session(session("ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("A002"));
    }

    @Test
    void adminCrudKeepsStatusCodesAndSelectionAndRestorationContract() throws Exception {
        when(schedules.getAllSchedules()).thenReturn(List.of(response()));
        when(schedules.createSchedule(any())).thenReturn(response());
        when(schedules.updateSchedule(eq(1L), any())).thenReturn(response());
        when(schedules.restoreSchedule(1L)).thenReturn(response());
        MockHttpSession admin = session("ROLE_ADMIN");

        mockMvc.perform(get(ADMIN_PATH).session(admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ddayEnabled").value(true));
        mockMvc.perform(post(ADMIN_PATH).with(csrf().asHeader()).session(admin).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.ddayEnabled").value(true));
        verify(schedules).createSchedule(argThat(input -> Boolean.TRUE.equals(input.getDdayEnabled())
                && input.getStartTime().getHour() == 9));
        mockMvc.perform(put(ADMIN_PATH + "/1").with(csrf().asHeader()).session(admin).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isOk());
        mockMvc.perform(delete(ADMIN_PATH + "/1").with(csrf().asHeader()).session(admin)).andExpect(status().isNoContent());
        mockMvc.perform(patch(ADMIN_PATH + "/1/restore").with(csrf().asHeader()).session(admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.ddayEnabled").value(true));
    }

    @Test
    void invertedPeriodIsACommonErrorAndLegacyDdayApiDoesNotExist() throws Exception {
        mockMvc.perform(post(ADMIN_PATH).with(csrf().asHeader()).session(session("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST.replace("2026-10-27", "2026-10-25")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("G002"));
        mockMvc.perform(get("/api/v1/ddays/active").session(session("ROLE_ADMIN")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/admin/ddays").session(session("ROLE_ADMIN")))
                .andExpect(status().isNotFound());
    }

    private ScheduleResponse response() {
        LocalDate day = LocalDate.of(2026, 10, 26);
        Schedule schedule = Schedule.builder().scheduleType("수강신청").startDate(day).endDate(day)
                .ddayEnabled(true).build();
        return ScheduleResponse.from(schedule, day);
    }

    private MockHttpSession session(String role) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new SessionPrincipal(1L, "admin@example.com").authentication(role));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        when(userIdentityQuery.findAuthenticatedUser(1L, "admin@example.com"))
                .thenReturn(Optional.of(new AuthenticatedUser(1L, "admin@example.com", role)));
        return session;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class,
            FlywayAutoConfiguration.class, RedisAutoConfiguration.class, RedisRepositoriesAutoConfiguration.class,
            SessionAutoConfiguration.class
    })
    @EnableMethodSecurity
    @Import({SecurityConfig.class, SecurityContextRepositoryConfig.class, SessionAuthenticationFilter.class,
            CustomAccessDeniedHandler.class, CustomAuthenticationEntryPoint.class, ScheduleController.class,
            AdminScheduleController.class, GlobalExceptionHandler.class})
    static class TestApplication {
    }
}
