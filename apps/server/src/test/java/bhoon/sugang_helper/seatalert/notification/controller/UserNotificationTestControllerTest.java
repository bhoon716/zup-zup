package bhoon.sugang_helper.seatalert.notification.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.service.UserNotificationTestService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class UserNotificationTestControllerTest {

    private static final String ONBOARDING_TEST_PATH = "/api/v1/users/notifications/test/onboarding";

    @ParameterizedTest
    @DisplayName("기존 테스트 알림 URL 두 개를 유지한다")
    @ValueSource(strings = {"/api/v1/notifications/test", "/api/v1/users/notifications/test"})
    void preservesBothExistingNotificationUrls(String path) throws Exception {
        // given
        UserNotificationTestService service = mock(UserNotificationTestService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new UserNotificationTestController(service)).build();

        // when & then
        mvc.perform(post(path)).andExpect(status().isOk());
        verify(service).sendTestNotification();
    }

    @Test
    @DisplayName("온보딩의 선택 채널을 테스트하고 본문 없는 200을 반환한다")
    void returnsEmptySuccessForSelectedDraftChannels() throws Exception {
        // given
        UserNotificationTestService service = mock(UserNotificationTestService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new UserNotificationTestController(service)).build();
        String email = "user@example.invalid";

        // when & then
        mvc.perform(post(ONBOARDING_TEST_PATH).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"notificationEmail":"%s","emailEnabled":true,"webPushEnabled":false,"discordEnabled":true}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
        verify(service).sendOnboardingTestNotification(email, List.of(NotificationChannel.EMAIL, NotificationChannel.DISCORD));
    }

    @Test
    @DisplayName("온보딩 테스트 요청에서 잘못된 이메일을 거부한다")
    void rejectsMalformedDraftEmail() throws Exception {
        // given
        UserNotificationTestService service = mock(UserNotificationTestService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new UserNotificationTestController(service)).build();

        // when & then
        mvc.perform(post(ONBOARDING_TEST_PATH).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"notificationEmail":"wrong","emailEnabled":true}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
