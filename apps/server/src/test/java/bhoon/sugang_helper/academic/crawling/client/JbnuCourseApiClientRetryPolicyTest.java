package bhoon.sugang_helper.academic.crawling.client;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.academic.crawling.enums.CrawlerFailureStage;
import bhoon.sugang_helper.academic.crawling.exception.CrawlerUpstreamFailure;
import java.io.IOException;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
        CrawlerUpstreamFailure nonRetryable = CrawlerUpstreamFailure.wrap(
                CrawlerFailureStage.COURSE_API,
                null,
                new IllegalArgumentException("malformed request"),
                false);

        // when & then
        assertThat(JbnuCourseApiClient.isTransientFailure(nonRetryable)).isFalse();
    }
}
