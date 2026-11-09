package bhoon.sugang_helper.academic.crawling.dto.diagnostic;

import bhoon.sugang_helper.academic.crawling.enums.CrawlerFailureStage;
import bhoon.sugang_helper.academic.crawling.exception.CrawlerUpstreamException;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.security.SensitiveDataRedactor;

public record CrawlerFailureSummary(CrawlerFailureStage stage, String failureType, String failureMessage,
                                    Integer upstreamStatus, Boolean retryable) {

    private static final int MAX_TYPE_LENGTH = 100;
    private static final int MAX_MESSAGE_LENGTH = 500;

    public static CrawlerFailureSummary from(CrawlerFailureStage stage, RuntimeException exception) {
        String message = "Unexpected crawler failure";
        if (exception instanceof CustomException customException) {
            message = customException.getErrorCode().getCode() + ": " + customException.getErrorCode().getMessage();
        }
        if (exception instanceof CrawlerUpstreamException upstream) {
            Throwable rootCause = CrawlerUpstreamException.rootCause(upstream);
            String type = truncate(SensitiveDataRedactor.exceptionType(rootCause), MAX_TYPE_LENGTH);
            message = message + "; stage=" + upstream.stage()
                    + "; upstreamStatus=" + upstreamStatusLabel(upstream.upstreamStatus())
                    + "; retryable=" + upstream.retryable();
            return new CrawlerFailureSummary(upstream.stage(), type, truncate(message, MAX_MESSAGE_LENGTH),
                    upstream.upstreamStatus(), upstream.retryable());
        }
        String type = truncate(SensitiveDataRedactor.exceptionType(exception), MAX_TYPE_LENGTH);
        return new CrawlerFailureSummary(stage, type, truncate(message, MAX_MESSAGE_LENGTH), null, null);
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
