package bhoon.sugang_helper.notification.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import bhoon.sugang_helper.common.util.EmailTemplateService;
import bhoon.sugang_helper.user.application.UserDeviceService;
import bhoon.sugang_helper.user.application.WebPushEndpointValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Provider requests must carry the same delivery-scoped, provider-safe key on every retry.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // The repeated key is the contract under test.
class NotificationSenderIdempotencyContractTest {

    private static final String IDEMPOTENCY_KEY = "c4Q4eVIRb9WX_qvYn9r1PQ";
    private static final String TITLE = "여석 발생";
    private static final String BODY = "빈자리가 생겼습니다.";

    @Test
    void fcmCarriesTheDeliveryKeyInTheDataPayload() throws Exception {
        FcmNotificationSender sender = new FcmNotificationSender();
        Method idempotentSend = idempotentSendMethod(sender);
        FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

        try (MockedStatic<FirebaseMessaging> firebaseMessagingStatic = org.mockito.Mockito.mockStatic(FirebaseMessaging.class)) {
            firebaseMessagingStatic.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
            when(firebaseMessaging.send(any(Message.class))).thenReturn("provider-message-id");

            invokeIdempotentSend(idempotentSend, sender, NotificationTarget.of("fcm-token"));

            verify(firebaseMessaging).send(messageCaptor.capture());
        }

        assertThat(fcmData(messageCaptor.getValue()))
                .containsEntry("idempotencyKey", IDEMPOTENCY_KEY);
    }

    @Test
    void emailCarriesTheDeliveryKeyInASafeCustomHeader() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailTemplateService templateService = mock(EmailTemplateService.class);
        EmailNotificationSender sender = new EmailNotificationSender(mailSender, templateService,
                "sender@example.com", "Sender");
        Method idempotentSend = idempotentSendMethod(sender);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateService.loadTemplate(any(), any())).thenReturn("<p>notification</p>");

        invokeIdempotentSend(idempotentSend, sender, NotificationTarget.of("recipient@example.com"));

        assertThat(mimeMessage.getHeader("X-Idempotency-Key"))
                .containsExactly(IDEMPOTENCY_KEY);
    }

    @Test
    void webPushCarriesTheDeliveryKeyInBothTopicAndEncryptedPayload() throws Exception {
        WebPushNotificationSender sender = new WebPushNotificationSender("", "", "",
                new ObjectMapper(), mock(UserDeviceService.class), new WebPushEndpointValidator());
        Method idempotentSend = idempotentSendMethod(sender);
        PushService pushService = mock(PushService.class);
        HttpResponse response = mock(HttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        when(pushService.send(notificationCaptor.capture())).thenReturn(response);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(201);
        sender.init();
        ReflectionTestUtils.setField(sender, "pushService", pushService);

        invokeIdempotentSend(idempotentSend, sender,
                NotificationTarget.ofWeb("https://fcm.googleapis.com/fcm/send/web-push-token",
                        generateP256dh(), generateAuth()));

        Notification sentNotification = notificationCaptor.getValue();
        JsonNode payload = new ObjectMapper().readTree(sentNotification.getPayload());
        assertThat(sentNotification.getTopic()).isEqualTo(IDEMPOTENCY_KEY);
        assertThat(payload.path("idempotencyKey").asText()).isEqualTo(IDEMPOTENCY_KEY);
    }

    @Test
    void discordUsesTheDeliveryKeyAsAnEnforcedNonce() throws Exception {
        DiscordNotificationSender sender = new DiscordNotificationSender();
        Method idempotentSend = idempotentSendMethod(sender);
        RestClient.Builder restClientBuilder = RestClient.builder().baseUrl("https://discord.com/api/v10");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        ReflectionTestUtils.setField(sender, "restClient", restClientBuilder.build());
        ReflectionTestUtils.setField(sender, "botToken", "bot-token");
        server.expect(requestTo("https://discord.com/api/v10/users/@me/channels"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bot bot-token"))
                .andRespond(withSuccess("{\"id\":\"dm-channel\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://discord.com/api/v10/channels/dm-channel/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bot bot-token"))
                .andExpect(content().json("""
                        {"content":"**여석 발생**\\n\\n빈자리가 생겼습니다.",
                         "nonce":"c4Q4eVIRb9WX_qvYn9r1PQ",
                         "enforce_nonce":true}
                        """))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        invokeIdempotentSend(idempotentSend, sender, NotificationTarget.of("discord-user"));

        server.verify();
    }

    private static Method idempotentSendMethod(NotificationSender sender) {
        try {
            return sender.getClass().getMethod("send", NotificationTarget.class, String.class, String.class,
                    String.class);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("NotificationSender must expose send(target, title, message, idempotencyKey)",
                    exception);
        }
    }

    private static void invokeIdempotentSend(Method method, NotificationSender sender, NotificationTarget target)
            throws Exception {
        try {
            method.invoke(sender, target, TITLE, BODY, IDEMPOTENCY_KEY);
        } catch (InvocationTargetException exception) {
            throw new AssertionError("idempotent notification dispatch failed", exception.getCause());
        }
    }

    @SuppressWarnings("unchecked") // Firebase's data accessor is deliberately package-private.
    private static Map<String, String> fcmData(Message message) {
        return (Map<String, String>) ReflectionTestUtils.getField(message, "data");
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
