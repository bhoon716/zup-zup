package bhoon.sugang_helper.crawling.application;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.crawling.domain.CrawlerFailureStage;
import bhoon.sugang_helper.crawling.infra.CrawlerUpstreamException;
import org.jsoup.HttpStatusException;
import org.junit.jupiter.api.Test;

class CrawlerFailureSummaryTest {

    @Test
    void upstreamFailureSummaryPreservesSafeStageStatusAndRetryability() {
        CrawlerUpstreamException upstream = CrawlerUpstreamException.wrap(
                CrawlerFailureStage.COURSE_API,
                new HttpStatusException("upstream response", 503, "https://jump.example.test/secret"),
                true);
        CustomException failure = new CustomException(ErrorCode.CRAWLER_CONNECTION_ERROR);
        failure.initCause(upstream);

        CrawlerFailureSummary summary = CrawlerFailureSummary.from(CrawlerFailureStage.FETCH_PARSE, failure);

        assertThat(summary.stage()).isEqualTo(CrawlerFailureStage.COURSE_API);
        assertThat(summary.failureType()).isEqualTo("HttpStatusException");
        assertThat(summary.failureMessage())
                .contains("C001")
                .contains("stage=COURSE_API")
                .contains("upstreamStatus=503")
                .contains("retryable=true")
                .doesNotContain("jump.example.test", "secret");
        assertThat(summary.diagnostic())
                .isEqualTo("failureStage=COURSE_API failureType=HttpStatusException "
                        + "upstreamStatus=503 retryable=true");
    }

    @Test
    void responseStatusFailureRetainsHttpStatusWithoutResponseBody() {
        CrawlerUpstreamException upstream = CrawlerUpstreamException.withStatus(
                CrawlerFailureStage.COURSE_API,
                429,
                new RuntimeException("response body must not be stored"),
                true);
        CustomException failure = new CustomException(ErrorCode.CRAWLER_CONNECTION_ERROR);
        failure.initCause(upstream);

        CrawlerFailureSummary summary = CrawlerFailureSummary.from(CrawlerFailureStage.FETCH_PARSE, failure);

        assertThat(summary.stage()).isEqualTo(CrawlerFailureStage.COURSE_API);
        assertThat(summary.upstreamStatus()).isEqualTo(429);
        assertThat(summary.failureMessage())
                .contains("upstreamStatus=429")
                .doesNotContain("response body must not be stored");
    }
}
