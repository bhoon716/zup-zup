package bhoon.sugang_helper.crawling.application;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.security.util.SensitiveDataRedactor;
import bhoon.sugang_helper.crawling.domain.CrawlerFailureStage;

public record CrawlerFailureSummary(CrawlerFailureStage stage, String failureType, String failureMessage) {

    private static final int MAX_TYPE_LENGTH = 100;
    private static final int MAX_MESSAGE_LENGTH = 500;

    public static CrawlerFailureSummary from(CrawlerFailureStage stage, RuntimeException exception) {
        String type = truncate(SensitiveDataRedactor.exceptionType(exception), MAX_TYPE_LENGTH);
        String message = exception instanceof CustomException customException
                ? customException.getErrorCode().getCode() + ": " + customException.getErrorCode().getMessage()
                : "Unexpected crawler failure";
        return new CrawlerFailureSummary(stage, type, truncate(message, MAX_MESSAGE_LENGTH));
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
