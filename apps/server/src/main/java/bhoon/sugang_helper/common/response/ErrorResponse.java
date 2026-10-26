package bhoon.sugang_helper.common.response;

import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.security.util.SensitiveDataRedactor;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@JsonInclude(Include.NON_NULL)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorResponse {

    private final String timestamp;

    private final String path;

    private final int status;

    private final String code;

    private final String message;

    private final String details;

    private final String correlationId;

    public static ErrorResponse of(ErrorCode errorCode, String path) {
        String mdcCorrelationId = org.slf4j.MDC.get("correlationId");
        String correlationId = (mdcCorrelationId != null && !mdcCorrelationId.isBlank())
                ? mdcCorrelationId
                : UUID.randomUUID().toString();

        return new ErrorResponse(
                LocalDateTime.now().toString(),
                SensitiveDataRedactor.redactPath(path),
                errorCode.getStatus().value(),
                errorCode.getCode(),
                errorCode.getMessage(),
                null,
                correlationId);
    }
}
