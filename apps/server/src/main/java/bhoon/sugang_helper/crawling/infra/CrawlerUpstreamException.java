package bhoon.sugang_helper.crawling.infra;

import bhoon.sugang_helper.crawling.domain.CrawlerFailureStage;
import java.io.IOException;
import org.jsoup.HttpStatusException;

/**
 * JUMP 요청 실패에 대한 운영 진단 정보와 원인 예외를 보존합니다.
 */
public final class CrawlerUpstreamException extends IOException {

    private final CrawlerFailureStage stage;
    private final Integer upstreamStatus;
    private final boolean retryable;

    private CrawlerUpstreamException(CrawlerFailureStage stage, Integer upstreamStatus, boolean retryable,
                                     Throwable cause) {
        super("JUMP upstream request failed", cause);
        this.stage = stage;
        this.upstreamStatus = upstreamStatus;
        this.retryable = retryable;
    }

    public static CrawlerUpstreamException wrap(CrawlerFailureStage stage, Throwable failure, boolean retryable) {
        if (failure instanceof CrawlerUpstreamException upstream && upstream.stage == stage) {
            return upstream;
        }
        return new CrawlerUpstreamException(stage, statusCodeOf(failure), retryable, failure);
    }

    public static CrawlerUpstreamException withStatus(CrawlerFailureStage stage, int statusCode,
                                                      Throwable failure, boolean retryable) {
        return new CrawlerUpstreamException(stage, statusCode, retryable, failure);
    }

    public static CrawlerUpstreamException find(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof CrawlerUpstreamException upstream) {
                return upstream;
            }
            current = current.getCause();
        }
        return null;
    }

    public static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private static Integer statusCodeOf(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof HttpStatusException statusException) {
                return statusException.getStatusCode();
            }
            current = current.getCause();
        }
        return null;
    }

    public CrawlerFailureStage stage() {
        return stage;
    }

    public Integer upstreamStatus() {
        return upstreamStatus;
    }

    public boolean retryable() {
        return retryable;
    }
}
