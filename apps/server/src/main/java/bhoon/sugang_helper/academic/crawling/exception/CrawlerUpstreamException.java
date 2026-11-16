package bhoon.sugang_helper.academic.crawling.exception;

import bhoon.sugang_helper.academic.crawling.enums.CrawlerFailureStage;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;

public final class CrawlerUpstreamException extends CustomException {

    private final CrawlerFailureStage stage;
    private final Integer upstreamStatus;
    private final boolean retryable;

    public CrawlerUpstreamException(CrawlerFailureStage stage, Integer upstreamStatus, boolean retryable,
                                    Throwable cause) {
        super(ErrorCode.CRAWLER_CONNECTION_ERROR);
        initCause(cause);
        this.stage = stage;
        this.upstreamStatus = upstreamStatus;
        this.retryable = retryable;
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
