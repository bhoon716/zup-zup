package bhoon.sugang_helper.notification.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.user.application.UserDeviceService;
import bhoon.sugang_helper.user.application.WebPushEndpointValidator;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.stream.Collectors;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WebPushNotificationSenderTest {

    private WebPushNotificationSender webPushNotificationSender;

    @BeforeEach
    void setUp() {
        webPushNotificationSender = new WebPushNotificationSender("", "", "",
                new ObjectMapper(),
                Mockito.mock(UserDeviceService.class),
                new WebPushEndpointValidator());
    }

    @Test
    @DisplayName("WEB 채널 지원 여부 확인")
    void supports_web_channel() {
        assertThat(webPushNotificationSender.supports(NotificationChannel.WEB)).isTrue();
        assertThat(webPushNotificationSender.supports(NotificationChannel.FCM)).isFalse();
    }

    @Test
    @DisplayName("Web Push 성공 로그에 endpoint 원문을 남기지 않는다")
    void send_doesNotLogEndpoint() throws Exception {
        String endpoint = "https://fcm.googleapis.com/fcm/send/endpoint-should-not-appear";
        PushService pushService = Mockito.mock(PushService.class);
        HttpResponse response = Mockito.mock(HttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);
        when(pushService.send(any(Notification.class))).thenReturn(response);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(201);
        webPushNotificationSender.init();
        ReflectionTestUtils.setField(webPushNotificationSender, "pushService", pushService);
        Logger logger = (Logger) LoggerFactory.getLogger(WebPushNotificationSender.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            webPushNotificationSender.send(NotificationTarget.ofWeb(endpoint, generateP256dh(), generateAuth()),
                    "제목", "내용");

            String messages = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .collect(Collectors.joining("\n"));
            assertThat(messages).doesNotContain(endpoint).contains("endpointFingerprint=sha256:");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private static String generateP256dh() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
        ECPublicKey publicKey = (ECPublicKey) keyPairGenerator.generateKeyPair().getPublic();
        byte[] encoded = new byte[65];
        encoded[0] = 4;
        copyCoordinate(publicKey.getW().getAffineX(), encoded, 1);
        copyCoordinate(publicKey.getW().getAffineY(), encoded, 33);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(encoded);
    }

    private static String generateAuth() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[16]);
    }

    private static void copyCoordinate(BigInteger coordinate, byte[] target, int offset) {
        byte[] source = coordinate.toByteArray();
        int copiedLength = Math.min(source.length, 32);
        System.arraycopy(source, source.length - copiedLength, target, offset + 32 - copiedLength, copiedLength);
    }
}
