package bhoon.sugang_helper.common.response;

import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.security.util.SensitiveDataRedactor;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@JsonInclude(Include.NON_NULL)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "에러 응답 포맷")
public class ErrorResponse {

    @Schema(description = "발생 시간", example = "2024-01-01T12:00:00")
    private final String timestamp;

    @Schema(description = "요청 경로", example = "/api/v1/auth/login")
    private final String path;

    @Schema(description = "HTTP 상태 코드", example = "400")
    private final int status;

    @Schema(description = "비즈니스 에러 코드", example = "U001")
    private final String code;

    @Schema(description = "에러 메시지", example = "사용자를 찾을 수 없습니다.")
    private final String message;

    @Schema(description = "상세 에러 정보 (필드 에러 등)")
    private final String details;

    @Schema(description = "서버 로그와 대조할 수 있는 오류 ID")
    private final String correlationId;

    public static ErrorResponse of(ErrorCode errorCode, String path) {
        return new ErrorResponse(
                LocalDateTime.now().toString(),
                SensitiveDataRedactor.redactPath(path),
                errorCode.getStatus().value(),
                errorCode.getCode(),
                errorCode.getMessage(),
                null,
                UUID.randomUUID().toString());
    }
}
