package bhoon.sugang_helper.academic.crawling.dto;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.academic.crawling.enums.CrawlerFailureStage;
import bhoon.sugang_helper.academic.crawling.exception.CrawlerUpstreamFailure;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientResponseException;

class CrawlerFailureSummaryTest {

    @Test
    void upstreamFailureSummaryPreservesSafeStageStatusAndRetryability() {
        // given
        CrawlerUpstreamFailure upstream = CrawlerUpstreamFailure.wrap(
                CrawlerFailureStage.COURSE_API, 503,
                new RestClientResponseException("https://jump.example.test/secret", 503,
                        "Service Unavailable", null, new byte[0], null),
                true);
        CustomException failure = new CustomException(ErrorCode.CRAWLER_CONNECTION_ERROR);
        failure.initCause(upstream);

        // when
        CrawlerFailureSummary summary = CrawlerFailureSummary.from(CrawlerFailureStage.FETCH_PARSE, failure);

        // then
        assertThat(summary.stage()).isEqualTo(CrawlerFailureStage.COURSE_API);
        assertThat(summary.failureType()).isEqualTo("RestClientResponseException");
        assertThat(summary.failureMessage())
                .contains("C001")
                .contains("stage=COURSE_API")
                .contains("upstreamStatus=503")
                .contains("retryable=true")
                .doesNotContain("jump.example.test", "secret");
        assertThat(summary.diagnostic())
                .isEqualTo("failureStage=COURSE_API failureType=RestClientResponseException "
                        + "upstreamStatus=503 retryable=true");
    }

    @Test
    void responseStatusFailureRetainsHttpStatusWithoutResponseBody() {
        // given
        CrawlerUpstreamFailure upstream = CrawlerUpstreamFailure.wrap(
                CrawlerFailureStage.COURSE_API,
                429,
                new RuntimeException("response body must not be stored"),
                true);
        CustomException failure = new CustomException(ErrorCode.CRAWLER_CONNECTION_ERROR);
        failure.initCause(upstream);

        // when
        CrawlerFailureSummary summary = CrawlerFailureSummary.from(CrawlerFailureStage.FETCH_PARSE, failure);

        // then
        assertThat(summary.stage()).isEqualTo(CrawlerFailureStage.COURSE_API);
        assertThat(summary.upstreamStatus()).isEqualTo(429);
        assertThat(summary.failureMessage())
                .contains("upstreamStatus=429")
                .doesNotContain("response body must not be stored");
    }
}
