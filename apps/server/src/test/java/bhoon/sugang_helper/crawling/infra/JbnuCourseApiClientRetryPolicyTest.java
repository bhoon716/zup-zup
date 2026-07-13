package bhoon.sugang_helper.crawling.infra;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(JbnuCourseApiClient.isTransientFailure(new IllegalArgumentException("malformed XML"))).isFalse();
    }
}
