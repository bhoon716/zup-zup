package bhoon.sugang_helper.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

@Getter
@JsonInclude(Include.NON_NULL)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "공통 응답 포맷")
public class CommonResponse<T> {

    private static final String SUCCESS_CODE = "SUCCESS";

    @Schema(description = "응답 코드", example = "SUCCESS")
    private final String code;

    @Schema(description = "응답 메시지", example = "요청 성공")
    private final String message;

    @Schema(description = "응답 데이터 (각 API별 데이터 객체)")
    private final T data;

    public static <T> CommonResponse<T> success(T data, String message) {
        return new CommonResponse<>(
                SUCCESS_CODE,
                message,
                data);
    }

    private static <T> CommonResponse<T> noContentBody(String message) {
        return new CommonResponse<>(
                SUCCESS_CODE,
                message,
                null);
    }

    public static <T> ResponseEntity<CommonResponse<T>> ok(T data, String message) {
        return ResponseEntity.ok(CommonResponse.success(data, message));
    }

    public static <T> ResponseEntity<CommonResponse<T>> created(T data, URI location, String message) {
        return ResponseEntity.created(location).body(CommonResponse.success(data, message));
    }

    public static <T> ResponseEntity<CommonResponse<T>> noContent(String message) {
        return ResponseEntity.ok(CommonResponse.noContentBody(message));
    }
}
