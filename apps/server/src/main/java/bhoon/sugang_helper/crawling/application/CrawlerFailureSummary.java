package bhoon.sugang_helper.crawling.application;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.security.util.SensitiveDataRedactor;
import bhoon.sugang_helper.crawling.domain.CrawlerFailureStage;
import bhoon.sugang_helper.crawling.infra.CrawlerUpstreamException;

public record CrawlerFailureSummary(CrawlerFailureStage stage, String failureType, String failureMessage,
                                    Integer upstreamStatus, Boolean retryable) {

    private static final int MAX_TYPE_LENGTH = 100;
    private static final int MAX_MESSAGE_LENGTH = 500;

    public static CrawlerFailureSummary from(CrawlerFailureStage stage, RuntimeException exception) {
        CrawlerUpstreamException upstream = CrawlerUpstreamException.find(exception);
        Throwable rootCause = upstream == null ? exception : CrawlerUpstreamException.rootCause(upstream);
        CrawlerFailureStage effectiveStage = upstream == null ? stage : upstream.stage();
        String type = truncate(SensitiveDataRedactor.exceptionType(rootCause), MAX_TYPE_LENGTH);
        String message = exception instanceof CustomException customException
                ? customException.getErrorCode().getCode() + ": " + customException.getErrorCode().getMessage()
                : "Unexpected crawler failure";
        if (upstream != null) {
            message = message + "; stage=" + effectiveStage
                    + "; upstreamStatus=" + upstreamStatusLabel(upstream.upstreamStatus())
                    + "; retryable=" + upstream.retryable();
        }
        return new CrawlerFailureSummary(effectiveStage, type, truncate(message, MAX_MESSAGE_LENGTH),
                upstream == null ? null : upstream.upstreamStatus(),
                upstream == null ? null : upstream.retryable());
    }

    public boolean hasUpstreamMetadata() {
        return retryable != null;
    }

    public String diagnostic() {
        if (!hasUpstreamMetadata()) {
            return "";
        }
        return "failureStage=" + stage
                + " failureType=" + failureType
                + " upstreamStatus=" + upstreamStatusLabel(upstreamStatus)
                + " retryable=" + retryable;
    }

    public String upstreamStatusLabel() {
        return upstreamStatusLabel(upstreamStatus);
    }

    public String retryableLabel() {
        return retryable == null ? "UNKNOWN" : retryable.toString();
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static String upstreamStatusLabel(Integer status) {
        return status == null ? "UNKNOWN" : status.toString();
    }
}
