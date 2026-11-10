package bhoon.sugang_helper.academic.crawling.dto.diagnostic;

import bhoon.sugang_helper.academic.crawling.enums.CrawlerFailureStage;
import bhoon.sugang_helper.academic.crawling.exception.CrawlerUpstreamException;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.security.SensitiveDataRedactor;
import java.util.Objects;

public record CrawlerFailureSummary(CrawlerFailureStage stage, String failureType, String failureMessage,
                                    Integer upstreamStatus, Boolean retryable) {

    private static final String UNKNOWN_LABEL = "UNKNOWN";
    private static final int MAX_TYPE_LENGTH = 100;
    private static final int MAX_MESSAGE_LENGTH = 500;

    public static CrawlerFailureSummary from(CrawlerFailureStage stage, RuntimeException exception) {
        String message = "강의 수집 중 예상하지 못한 오류가 발생했습니다.";
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
        return Objects.toString(retryable, UNKNOWN_LABEL);
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static String upstreamStatusLabel(Integer status) {
        return Objects.toString(status, UNKNOWN_LABEL);
    }
}
