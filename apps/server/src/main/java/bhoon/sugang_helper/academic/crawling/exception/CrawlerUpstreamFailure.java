package bhoon.sugang_helper.academic.crawling.exception;

import bhoon.sugang_helper.academic.crawling.enums.CrawlerFailureStage;
import java.io.IOException;




public class CrawlerUpstreamFailure extends IOException {

    private final CrawlerFailureStage stage;
    private final Integer upstreamStatus;
    private final boolean retryable;

    protected CrawlerUpstreamFailure(CrawlerFailureStage stage, Integer upstreamStatus, boolean retryable,
                                     Throwable cause) {
        super("JUMP upstream request failed", cause);
        this.stage = stage;
        this.upstreamStatus = upstreamStatus;
        this.retryable = retryable;
    }

    public static CrawlerUpstreamFailure wrap(CrawlerFailureStage stage, Integer upstreamStatus,
                                               Throwable failure, boolean retryable) {
        if (failure instanceof CrawlerUpstreamFailure upstream && upstream.stage == stage) {
            return upstream;
        }
        return new CrawlerUpstreamFailure(stage, upstreamStatus, retryable, failure);
    }

    public static CrawlerUpstreamFailure find(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof CrawlerUpstreamFailure upstream) {
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
