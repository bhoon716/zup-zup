package bhoon.sugang_helper.domain.notification.sender;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FcmNotificationSenderTest {

    private FcmNotificationSender fcmNotificationSender;

    @BeforeEach
    void setUp() {
        fcmNotificationSender = new FcmNotificationSender();
    }

    @Test
    @DisplayName("FCM 채널 지원 여부 확인")
    void supports_fcm_channel() {
        assertThat(fcmNotificationSender.supports(NotificationChannel.FCM)).isTrue();
        assertThat(fcmNotificationSender.supports(NotificationChannel.EMAIL)).isFalse();
    }
}
