package bhoon.sugang_helper.academic.crawling.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bhoon.sugang_helper.academic.crawling.enums.CrawlerFailureStage;
import bhoon.sugang_helper.academic.crawling.exception.CrawlerUpstreamException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientResponseException;

class JbnuCourseApiClientRetryPolicyTest {

    @Test
    @DisplayName("일시적인 상위 서버 오류는 재시도 대상으로 분류한다")
    void transientUpstreamFailuresAreRetryable() {
        // when & then
        assertThat(JbnuCourseApiClient.isTransientFailure(new SocketTimeoutException("timeout"))).isTrue();
        assertThat(JbnuCourseApiClient.isTransientFailure(new IOException("connection reset"))).isTrue();
    }

    @Test
    @DisplayName("잘못된 응답 본문은 전송 오류로 재시도하지 않는다")
    void malformedPayloadFailuresAreNotRetriedAsTransportFailures() {
        // when & then
        assertThat(JbnuCourseApiClient.isTransientFailure(new IllegalArgumentException("malformed JSON"))).isFalse();
    }

    @Test
    @DisplayName("감싸진 오류도 명시된 재시도 분류를 유지한다")
    void wrappedFailureUsesItsExplicitRetryClassification() {
        // given
        CrawlerUpstreamException nonRetryable = new CrawlerUpstreamException(
                CrawlerFailureStage.COURSE_API, null, false, new IllegalArgumentException("malformed request"));

        // when & then
        assertThat(JbnuCourseApiClient.isTransientFailure(nonRetryable)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = CrawlerFailureStage.class, names = {"JUMP_BOOTSTRAP", "COURSE_API"})
    @DisplayName("재시도 소진 후에도 실패 단계와 학교 응답 상태를 담은 C001 예외를 그대로 던진다")
    void retriesPreserveUpstreamFailureDetails(CrawlerFailureStage failureStage) throws IOException {
        // given
        AtomicInteger failedRequests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/bootstrap", exchange -> {
            try (exchange) {
                if (failureStage == CrawlerFailureStage.JUMP_BOOTSTRAP) {
                    failedRequests.incrementAndGet();
                    exchange.sendResponseHeaders(503, -1);
                    return;
                }
                exchange.getResponseHeaders().add("Set-Cookie", "JSESSIONID=test; Path=/");
                exchange.sendResponseHeaders(200, -1);
            }
        });
        server.createContext("/course", exchange -> {
            try (exchange) {
                exchange.getRequestBody().readAllBytes();
                failedRequests.incrementAndGet();
                exchange.sendResponseHeaders(503, -1);
            }
        });
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        JbnuCourseApiClient client = new JbnuCourseApiClient();
        ReflectionTestUtils.setField(client, "bootstrapUrl", baseUrl + "/bootstrap");
        ReflectionTestUtils.setField(client, "apiUrl", baseUrl + "/course");
        ReflectionTestUtils.setField(client, "timeoutMs", 5_000);
        ReflectionTestUtils.setField(client, "maxRetries", 1);
        ReflectionTestUtils.setField(client, "retryWaitMs", 0);
        server.start();

        // when & then
        try {
            assertThatThrownBy(() -> client.fetchCourseDataStream("2026", "U211600020", "6"))
                    .hasRootCauseInstanceOf(RestClientResponseException.class)
                    .isInstanceOfSatisfying(CrawlerUpstreamException.class, failure -> {
                        assertThat(failure.getErrorCode()).isEqualTo(ErrorCode.CRAWLER_CONNECTION_ERROR);
                        assertThat(failure.stage()).isEqualTo(failureStage);
                        assertThat(failure.upstreamStatus()).isEqualTo(503);
                        assertThat(failure.retryable()).isTrue();
                    });
            assertThat(failedRequests).hasValue(2);
        } finally {
            server.stop(0);
        }
    }
}
