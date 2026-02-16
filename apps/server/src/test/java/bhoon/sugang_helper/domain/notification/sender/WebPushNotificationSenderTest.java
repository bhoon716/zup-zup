package bhoon.sugang_helper.domain.notification.sender;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.domain.user.service.UserDeviceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebPushNotificationSenderTest {

    private WebPushNotificationSender webPushNotificationSender;

    @BeforeEach
    void setUp() {
        webPushNotificationSender = new WebPushNotificationSender("", "", "",
                new ObjectMapper(),
                Mockito.mock(UserDeviceService.class));
    }

    @Test
    @DisplayName("WEB 채널 지원 여부 확인")
    void supports_web_channel() {
        assertThat(webPushNotificationSender.supports(NotificationChannel.WEB)).isTrue();
        assertThat(webPushNotificationSender.supports(NotificationChannel.FCM)).isFalse();
    }
}
