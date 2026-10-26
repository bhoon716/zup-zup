package bhoon.sugang_helper.seatalert.notification.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import bhoon.sugang_helper.common.config.MdcTaskDecorator;
import bhoon.sugang_helper.identity.user.service.UserDeviceWebPushManagement;
import bhoon.sugang_helper.seatalert.enums.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.dto.provider.NotificationTarget;
import bhoon.sugang_helper.seatalert.notification.exception.NotificationProviderException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.lang.JoseException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class WebPushHttpCancellationTest {

    @Test
    void documentsThatOuterTimeoutLeavesTheRealWebPushHttpRequestRunning() throws Exception {
        // given: real encryption and HTTP transport, with only endpoint validation bypassed for localhost.
        Security.addProvider(new BouncyCastleProvider());
        ObservedPushService pushService = new ObservedPushService(generateKeyPair());
        WebPushNotificationSender sender = new WebPushNotificationSender("", "", "",
                new ObjectMapper(), mock(UserDeviceWebPushManagement.class));
        ReflectionTestUtils.setField(sender, "pushService", pushService);
        CountDownLatch requestReceived = new CountDownLatch(1);
        CountDownLatch releaseResponse = new CountDownLatch(1);
        AtomicInteger encryptedRequestBytes = new AtomicInteger();
        ExecutorService serverExecutor = Executors.newSingleThreadExecutor();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(serverExecutor);
        server.createContext("/push", exchange -> {
            try {
                encryptedRequestBytes.set(exchange.getRequestBody().readAllBytes().length);
                requestReceived.countDown();
                if (!releaseResponse.await(10, TimeUnit.SECONDS)) {
                    throw new IOException("The local delayed response was not released");
                }
                exchange.sendResponseHeaders(201, -1);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        KeyPair receiver = generateKeyPair();
        String receiverKey = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(((ECPublicKey) receiver.getPublic()).getQ().getEncoded(false));
        String auth = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[16]);
        String endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/push";

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        try (NotificationProviderResilience resilience = new NotificationProviderResilience(
                registry, 5_000, 3, 30, new MdcTaskDecorator())) {
            server.start();
            long startedAt = System.nanoTime();

            // when: the production five-second wait includes sender payload creation and SDK encryption.
            assertThatThrownBy(() -> resilience.execute(NotificationChannel.WEB,
                    () -> sender.send(NotificationTarget.ofWeb(endpoint, receiverKey, auth), "title", "body")))
                    .isInstanceOf(NotificationProviderException.class)
                    .hasFieldOrPropertyWithValue("reason", "TIMEOUT");
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

            // then: this characterizes a limitation; update these expectations if cancellation is later propagated.
            assertThat(requestReceived.getCount()).isZero();
            assertThat(encryptedRequestBytes.get()).isPositive();
            Future<HttpResponse> httpRequest = pushService.request();
            assertThat(httpRequest).isNotNull();
            boolean cancelledAtTimeout = httpRequest.isCancelled();
            boolean completedAtTimeout = httpRequest.isDone();
            assertThat(cancelledAtTimeout).isFalse();
            assertThat(completedAtTimeout).isFalse();
            releaseResponse.countDown();
            int lateStatus = httpRequest.get(3, TimeUnit.SECONDS).getStatusLine().getStatusCode();
            assertThat(lateStatus).isEqualTo(201);
            System.out.printf("webpush_http_cancel_probe configuredTimeoutMs=5000 elapsedMs=%d "
                            + "encryptedRequestBytes=%d lowerCancelledAtTimeout=%s lowerDoneAtTimeout=%s lateStatus=%d%n",
                    elapsedMs, encryptedRequestBytes.get(), cancelledAtTimeout, completedAtTimeout, lateStatus);
        } finally {
            releaseResponse.countDown();
            Future<HttpResponse> request = pushService.request();
            if (request != null && !request.isDone()) {
                request.cancel(true);
            }
            server.stop(0);
            serverExecutor.shutdownNow();
            registry.close();
        }
    }

    private static KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return generator.generateKeyPair();
    }

    private static final class ObservedPushService extends PushService {

        private final AtomicReference<Future<HttpResponse>> request = new AtomicReference<>();

        private ObservedPushService(KeyPair keyPair) {
            super(keyPair, "mailto:probe@example.invalid");
        }

        @Override
        @SuppressWarnings("deprecation")
        public Future<HttpResponse> sendAsync(Notification notification, Encoding encoding)
                throws GeneralSecurityException, IOException, JoseException {
            Future<HttpResponse> result = super.sendAsync(notification, encoding);
            request.set(result);
            return result;
        }

        private Future<HttpResponse> request() {
            return request.get();
        }
    }
}
