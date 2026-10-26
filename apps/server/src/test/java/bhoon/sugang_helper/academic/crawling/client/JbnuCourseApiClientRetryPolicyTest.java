package bhoon.sugang_helper.academic.crawling.client;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.academic.crawling.enums.CrawlerFailureStage;
import bhoon.sugang_helper.academic.crawling.exception.CrawlerUpstreamFailure;
import java.io.IOException;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;

class JbnuCourseApiClientRetryPolicyTest {

    @Test
    void transientUpstreamFailuresAreRetryable() {
        assertThat(JbnuCourseApiClient.isTransientFailure(new SocketTimeoutException("timeout"))).isTrue();
        assertThat(JbnuCourseApiClient.isTransientFailure(new IOException("connection reset"))).isTrue();
    }

    @Test
    void malformedPayloadFailuresAreNotRetriedAsTransportFailures() {
        assertThat(JbnuCourseApiClient.isTransientFailure(new IllegalArgumentException("malformed JSON"))).isFalse();
    }

    @Test
    void wrappedFailureUsesItsExplicitRetryClassification() {
        CrawlerUpstreamFailure nonRetryable = CrawlerUpstreamFailure.wrap(
                CrawlerFailureStage.COURSE_API,
                null,
                new IllegalArgumentException("malformed request"),
                false);

        assertThat(JbnuCourseApiClient.isTransientFailure(nonRetryable)).isFalse();
    }
}
