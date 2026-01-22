package bhoon.sugang_helper.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.net.URI;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

@Getter
@JsonInclude(Include.NON_NULL)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CommonResponse<T> {

    private static final String SUCCESS_CODE = "SUCCESS";

    private final String code;
    private final String message;
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
