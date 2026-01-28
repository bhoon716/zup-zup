package bhoon.sugang_helper.common.error;

import bhoon.sugang_helper.common.response.ErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final int MAX_VALIDATION_ERRORS_TO_LOG = 5;

    // Custom Exception
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(HttpServletRequest req, CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        String detail = e.getDetail();
        return response(req, errorCode, detail);
    }

    // Validation Error
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(HttpServletRequest req, MethodArgumentNotValidException e) {
        Map<String, Object> details = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().stream()
                .limit(MAX_VALIDATION_ERRORS_TO_LOG)
                .forEach(err -> details.put(err.getField(), err.getDefaultMessage()));

        return response(req, ErrorCode.INVALID_INPUT, details.toString());
    }

    // JWT Error
    @ExceptionHandler({ ExpiredJwtException.class, JwtException.class })
    public ResponseEntity<ErrorResponse> handleJwt(HttpServletRequest req, JwtException e) {
        return response(req, ErrorCode.INVALID_TOKEN, ErrorCode.INVALID_TOKEN.getMessage());
    }

    // Authorization Error
    @ExceptionHandler({ AuthorizationDeniedException.class, AccessDeniedException.class })
    public ResponseEntity<ErrorResponse> handleForbidden(HttpServletRequest req, RuntimeException e) {
        return response(req, ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.getMessage());
    }

    // Not Found Error
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(HttpServletRequest req, NoResourceFoundException e) {
        return response(req, ErrorCode.NOT_FOUND, ErrorCode.NOT_FOUND.getMessage());
    }

    // Database Error
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrity(HttpServletRequest req, DataIntegrityViolationException e) {
        return response(req, ErrorCode.INVALID_INPUT, "데이터 처리 중 충돌이 발생했습니다.");
    }

    // Internal Server Error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(HttpServletRequest req, Exception e) {
        return response(req, ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getMessage());
    }

    private ResponseEntity<ErrorResponse> response(HttpServletRequest req, ErrorCode errorCode, String details) {
        log(errorCode, req.getMethod(), req.getRequestURI(), details);
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, req.getRequestURI(), details));
    }

    private void log(ErrorCode errorCode, String method, String path, String details) {
        // [ERROR][CODE_A001] [POST /api/v1/...] [MESSAGE: ...] [DETAIL: ...]
        log.error("[ERROR][CODE_{}] [{} {}] [MESSAGE: {}] [DETAIL: {}]",
                errorCode.getCode(), method, path, errorCode.getMessage(), details);
    }
}
