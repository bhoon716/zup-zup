package bhoon.sugang_helper.common.error;

import bhoon.sugang_helper.common.response.ErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
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

    private static final int MAX_VALIDATION_ERRORS_TO_LOG = 10;

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(HttpServletRequest req, CustomException e) {
        ErrorCode code = e.getErrorCode();
        Map<String, Object> details = detailOrNull(e.getDetail());
        return respond(req, code, details, e);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(HttpServletRequest req, MethodArgumentNotValidException e) {
        Map<String, Object> details = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().stream()
                .limit(MAX_VALIDATION_ERRORS_TO_LOG)
                .forEach(err -> details.put(err.getField(), err.getDefaultMessage()));

        int total = e.getBindingResult().getFieldErrorCount();
        log.warn("[{}] {} {} validationErrors={} logged={}",
                ErrorCode.INVALID_INPUT.getCode(),
                req.getMethod(), req.getRequestURI(),
                total, details.size());

        return buildResponse(ErrorCode.INVALID_INPUT, req.getRequestURI(), details);
    }

    @ExceptionHandler({ExpiredJwtException.class, JwtException.class})
    public ResponseEntity<ErrorResponse> handleJwt(HttpServletRequest req, JwtException e) {
        String msg = (e instanceof ExpiredJwtException) ? "만료된 토큰입니다." : "유효하지 않은 토큰입니다.";
        return respond(req, ErrorCode.INVALID_TOKEN, detailOrNull(msg), e);
    }

    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleForbidden(HttpServletRequest req, RuntimeException e) {
        return respond(req, ErrorCode.FORBIDDEN, null, e);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(HttpServletRequest req, NoResourceFoundException e) {
        return respond(req, ErrorCode.NOT_FOUND, null, e);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrity(HttpServletRequest req, DataIntegrityViolationException e) {
        Throwable root = mostSpecificCause(e);
        Map<String, Object> details = detailOrNull("데이터 처리 중 충돌이 발생했습니다. (중복 데이터 등)");
        if (details != null) {
            details = new LinkedHashMap<>(details);
            details.put("cause", root.getClass().getSimpleName());
        }

        return respond(req, ErrorCode.INVALID_INPUT, details, e);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(HttpServletRequest req, Exception e) {
        Map<String, Object> details = detailOrNull("서버 오류가 발생했습니다.");
        return respond(req, ErrorCode.INTERNAL_ERROR, details, e);
    }

    private ResponseEntity<ErrorResponse> respond(HttpServletRequest req, ErrorCode code, Map<String, Object> details,
                                                  Exception e) {
        logCompact(req, code, e, details);
        return buildResponse(code, req.getRequestURI(), details);
    }

    private ResponseEntity<ErrorResponse> buildResponse(ErrorCode error, String path, Map<String, Object> details) {
        return ResponseEntity.status(error.getStatus())
                .body(ErrorResponse.of(error, path, details));
    }

    private void logCompact(HttpServletRequest req, ErrorCode code, Exception e, Map<String, Object> details) {
        HttpStatus status = code.getStatus();
        String ex = e.getClass().getSimpleName();

        if (status.is4xxClientError()) {
            log.warn("[{}] {} {} ex={}",
                    code.getCode(), req.getMethod(), req.getRequestURI(), ex);
            return;
        }

        if (log.isDebugEnabled()) {
            log.error("[{}] {} {} ex={}",
                    code.getCode(), req.getMethod(), req.getRequestURI(), ex, e);
        } else {
            log.error("[{}] {} {} ex={}",
                    code.getCode(), req.getMethod(), req.getRequestURI(), ex);
        }
    }

    private Map<String, Object> detailOrNull(String msg) {
        return (msg == null || msg.isBlank()) ? null : Map.of("detail", msg);
    }

    private Throwable mostSpecificCause(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur;
    }
}
