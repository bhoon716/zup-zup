package bhoon.sugang_helper.seatalert.notification.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.api.security.SensitiveDataRedactor;
import bhoon.sugang_helper.identity.user.service.UserDeviceService;
import bhoon.sugang_helper.identity.user.service.WebPushEndpointValidator;
import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.dto.provider.NotificationTarget;
import bhoon.sugang_helper.seatalert.notification.exception.NotificationProviderException;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicHttpResponse;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WebPushNotificationSenderTest {

    private WebPushNotificationSender webPushNotificationSender;
    private UserDeviceService devices;

    @BeforeEach
    void setUp() {
        devices = Mockito.mock(UserDeviceService.class);
        webPushNotificationSender = new WebPushNotificationSender("", "", "", new ObjectMapper(), devices);
    }

    @Test
    @DisplayName("WEB 채널 지원 여부 확인")
    void supports_web_channel() {
        // when & then
        assertThat(webPushNotificationSender.supports(NotificationChannel.WEB)).isTrue();
        assertThat(webPushNotificationSender.supports(NotificationChannel.EMAIL)).isFalse();
    }

    @Test
    @DisplayName("Web Push 성공 로그에 endpoint 원문을 남기지 않는다")
    @SuppressWarnings("deprecation")
    void send_doesNotLogEndpoint() throws Exception {
        // given
        String endpoint = "https://fcm.googleapis.com/fcm/send/endpoint-should-not-appear";
        PushService pushService = Mockito.mock(PushService.class);

        // when
        HttpResponse response = Mockito.mock(HttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);
        when(pushService.sendAsync(any(Notification.class), eq(Encoding.AESGCM)))
                .thenReturn(CompletableFuture.completedFuture(response));
        when(response.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(201);
        webPushNotificationSender.init();
        ReflectionTestUtils.setField(webPushNotificationSender, "pushService", pushService);
        Logger logger = (Logger) LoggerFactory.getLogger(WebPushNotificationSender.class);
        Level previousLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        // then
        try {
            webPushNotificationSender.send(NotificationTarget.ofWeb(endpoint, generateP256dh(), generateAuth()),
                    "제목", "내용");

            assertThat(appender.list).hasSize(2).allSatisfy(event -> {
                assertThat(event.getFormattedMessage()).doesNotContain(endpoint);
                assertThat(event.getKeyValuePairs().toString()).doesNotContain(endpoint);
                assertThat(event.getKeyValuePairs()).extracting(pair -> pair.key, pair -> pair.value)
                        .contains(tuple("endpointFingerprint", SensitiveDataRedactor.fingerprint(endpoint)));
            });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
            logger.setLevel(previousLevel);
        }
    }

    @Test
    @DisplayName("웹 푸시 발송은 받은 알림 경로를 포함하며 만료 구독 삭제를 정산에 맡긴다")
    @SuppressWarnings("deprecation")
    void durablePushRoutesToInboxAndLeavesExpiredDeviceForSettlement() throws Exception {
        // given
        String endpoint = "https://fcm.googleapis.com/fcm/send/expired";
        NotificationTarget target = NotificationTarget.ofWeb(endpoint, generateP256dh(), generateAuth());
        PushService pushService = Mockito.mock(PushService.class);
        HttpResponse response = Mockito.mock(HttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);
        when(pushService.sendAsync(any(Notification.class), eq(Encoding.AESGCM)))
                .thenReturn(CompletableFuture.completedFuture(response));
        when(response.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(410);
        webPushNotificationSender.init();
        ReflectionTestUtils.setField(webPushNotificationSender, "pushService", pushService);

        // when & then
        assertThatThrownBy(() -> webPushNotificationSender.sendDelivery(target, "제목", "내용", "key", "/notifications"))
                .isInstanceOf(NotificationProviderException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WEB_PUSH_INVALID_SUBSCRIPTION);
        verify(devices, never()).deleteDeviceByToken(endpoint);
        ArgumentCaptor<Notification> notification = ArgumentCaptor.forClass(Notification.class);
        verify(pushService).sendAsync(notification.capture(), eq(Encoding.AESGCM));
        String payload = new String(notification.getValue().getPayload(), StandardCharsets.UTF_8);
        assertThat(new ObjectMapper().readTree(payload).get("url").asText()).isEqualTo("/notifications");
    }

    @Test
    @DisplayName("Chromium 테스트 푸시 발송 요청을 원래 주소와 VAPID 대상으로 만든다")
    @SuppressWarnings("deprecation")
    void createsChromiumStagingPushRequest() throws Exception {
        // given
        String endpoint = "https://jmt17.google.com/fcm/send/client_id:fixture-token_1";
        UserDeviceService subscriptions = new UserDeviceService(null, null, new WebPushEndpointValidator());
        WebPushNotificationSender sender = new WebPushNotificationSender("", "", "", new ObjectMapper(), subscriptions);
        sender.init();

        PushService pushService = Mockito.mock(PushService.class);
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 201, "Created");
        when(pushService.sendAsync(any(Notification.class), eq(Encoding.AESGCM)))
                .thenReturn(CompletableFuture.completedFuture(response));
        ReflectionTestUtils.setField(sender, "pushService", pushService);

        // when
        sender.sendDelivery(NotificationTarget.ofWeb(endpoint, generateP256dh(), generateAuth()),
                "제목", "내용", "key", "/notifications");

        // then
        ArgumentCaptor<Notification> notification = ArgumentCaptor.forClass(Notification.class);
        verify(pushService).sendAsync(notification.capture(), eq(Encoding.AESGCM));
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
        keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
        PushService requestBuilder = new PushService(keyPairGenerator.generateKeyPair());
        HttpPost request = requestBuilder.preparePost(notification.getValue(), Encoding.AESGCM);
        assertThat(request.getURI()).isEqualTo(URI.create(endpoint));
        assertThat(request.getEntity().getContentLength()).isPositive();
        JsonWebSignature signature = new JsonWebSignature();
        signature.setCompactSerialization(request.getFirstHeader("Authorization").getValue().split(" ", 2)[1]);
        assertThat(JwtClaims.parse(signature.getUnverifiedPayload()).getAudience())
                .containsExactly("https://jmt17.google.com");
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
