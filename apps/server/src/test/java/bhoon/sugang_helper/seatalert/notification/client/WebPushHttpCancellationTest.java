package bhoon.sugang_helper.seatalert.notification.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import bhoon.sugang_helper.common.api.concurrent.MdcTaskDecorator;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.identity.api.notifications.WebPushSubscriptionManagement;
import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.dto.provider.NotificationTarget;
import bhoon.sugang_helper.seatalert.notification.exception.NotificationProviderException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class WebPushHttpCancellationTest {

    private static final String LOOPBACK_HOST = "127.0.0.1";
    private static final int APP_TIMEOUT_MS = 2_000;
    private static final int SOCKET_TIMEOUT_MS = 5_000;

    @Test
    @DisplayName("웹 푸시 타임아웃이 발생하면 응답을 기다리던 HTTP 연결을 닫는다")
    void timeoutClosesTheWebPushConnectionBeforeTheServerResponds() throws Exception {
        // given
        Security.addProvider(new BouncyCastleProvider());
        WebPushNotificationSender sender = new WebPushNotificationSender("", "", "",
                new ObjectMapper(), mock(WebPushSubscriptionManagement.class));
        ReflectionTestUtils.setField(sender, "pushService",
                new PushService(generateKeyPair(), "mailto:probe@example.invalid"));
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName(LOOPBACK_HOST));
             ExecutorService serverExecutor = Executors.newVirtualThreadPerTaskExecutor();
             NotificationProviderResilience resilience = new NotificationProviderResilience(
                     registry, APP_TIMEOUT_MS, 3, 30, new MdcTaskDecorator())) {
            server.setSoTimeout(SOCKET_TIMEOUT_MS);
            Future<Integer> nextByte = serverExecutor.submit(() -> readAfterRequestBody(server));
            NotificationTarget target = target(server.getLocalPort());

            // when & then
            assertThatThrownBy(() -> resilience.execute(NotificationChannel.WEB,
                    () -> sender.send(target, "title", "body")))
                    .isInstanceOf(NotificationProviderException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WEB_PUSH_SEND_ERROR)
                    .hasFieldOrPropertyWithValue("reason", "TIMEOUT");
            assertThat(nextByte.get(2, TimeUnit.SECONDS)).isEqualTo(-1);
        } finally {
            registry.close();
        }
    }

    private static int readAfterRequestBody(ServerSocket server) throws Exception {
        try (Socket socket = server.accept()) {
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);
            DefaultBHttpServerConnection connection = new DefaultBHttpServerConnection(4096);
            connection.bind(socket);
            HttpEntityEnclosingRequest request = (HttpEntityEnclosingRequest) connection.receiveRequestHeader();
            connection.receiveRequestEntity(request);
            assertThat(request.getEntity().getContent().readAllBytes()).isNotEmpty();
            return socket.getInputStream().read();
        }
    }

    private static NotificationTarget target(int port) throws GeneralSecurityException {
        KeyPair receiver = generateKeyPair();
        String receiverKey = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(((ECPublicKey) receiver.getPublic()).getQ().getEncoded(false));
        String auth = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[16]);
        return NotificationTarget.ofWeb("http://" + LOOPBACK_HOST + ":" + port + "/push", receiverKey, auth);
    }

    private static KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return generator.generateKeyPair();
    }
}
