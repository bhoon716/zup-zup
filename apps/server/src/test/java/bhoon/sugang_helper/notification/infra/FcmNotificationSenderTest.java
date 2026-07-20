package bhoon.sugang_helper.notification.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

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

    @Test
    @DisplayName("FCM 성공 로그에 토큰과 provider 응답 원문을 남기지 않는다")
    void send_doesNotLogTokenOrProviderResponse() throws Exception {
        String token = "fcm-token-should-not-appear";
        String providerResponse = "provider-response-should-not-appear";
        FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
        Logger logger = (Logger) LoggerFactory.getLogger(FcmNotificationSender.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try (MockedStatic<FirebaseMessaging> firebaseMessagingStatic = org.mockito.Mockito.mockStatic(FirebaseMessaging.class)) {
            firebaseMessagingStatic.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
            when(firebaseMessaging.send(any(Message.class))).thenReturn(providerResponse);

            fcmNotificationSender.send(NotificationTarget.of(token), "제목", "내용");

            String messages = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .collect(Collectors.joining("\n"));
            assertThat(messages).doesNotContain(token, providerResponse).contains("tokenFingerprint=sha256:");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
