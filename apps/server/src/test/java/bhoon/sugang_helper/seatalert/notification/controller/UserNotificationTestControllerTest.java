package bhoon.sugang_helper.seatalert.notification.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bhoon.sugang_helper.seatalert.notification.service.UserNotificationTestService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class UserNotificationTestControllerTest {

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/notifications/test", "/api/v1/users/notifications/test"})
    void preservesBothExistingNotificationUrls(String path) throws Exception {
        // given
        UserNotificationTestService service = mock(UserNotificationTestService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new UserNotificationTestController(service)).build();

        // when & then
        mvc.perform(post(path)).andExpect(status().isOk());
        verify(service).sendTestNotification();
    }
}
