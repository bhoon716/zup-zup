package bhoon.sugang_helper.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import bhoon.sugang_helper.common.error.ErrorCode;

@Getter
@JsonInclude(Include.NON_NULL)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorResponse {

    private final String timestamp;
    private final String path;
    private final int status;
    private final String code;
    private final String message;
    private final Map<String, Object> details;

    public static ErrorResponse of(ErrorCode errorCode, String path, Map<String, Object> details) {
        return new ErrorResponse(
                LocalDateTime.now().toString(),
                path,
                errorCode.getStatus().value(),
                errorCode.getCode(),
                errorCode.getMessage(),
                details);
    }
}
