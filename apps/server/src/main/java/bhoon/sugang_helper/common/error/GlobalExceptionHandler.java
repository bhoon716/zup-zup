package bhoon.sugang_helper.common.error;

import bhoon.sugang_helper.common.response.ErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
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

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(HttpServletRequest req, CustomException e) {
        return response(req, e.getErrorCode(), e);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(HttpServletRequest req, MethodArgumentNotValidException e) {
        return response(req, ErrorCode.INVALID_INPUT, e);
    }

    @ExceptionHandler({ExpiredJwtException.class, JwtException.class})
    public ResponseEntity<ErrorResponse> handleJwt(HttpServletRequest req, JwtException e) {
        return response(req, ErrorCode.INVALID_TOKEN, e);
    }

    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleForbidden(HttpServletRequest req, RuntimeException e) {
        return response(req, ErrorCode.FORBIDDEN, e);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(HttpServletRequest req, NoResourceFoundException e) {
        return response(req, ErrorCode.NOT_FOUND, e);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrity(HttpServletRequest req, DataIntegrityViolationException e) {
        return response(req, ErrorCode.INVALID_INPUT, e);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(HttpServletRequest req,
                                                               OptimisticLockingFailureException e) {
        return response(req, ErrorCode.CONCURRENT_MODIFICATION, e);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(HttpServletRequest req, Exception e) {
        return response(req, ErrorCode.INTERNAL_ERROR, e);
    }

    private ResponseEntity<ErrorResponse> response(HttpServletRequest req, ErrorCode errorCode, Exception exception) {
        ErrorResponse errorResponse = ErrorResponse.of(errorCode, req.getRequestURI());
        log(errorCode, req.getMethod(), errorResponse.getPath(), errorResponse.getCorrelationId(), exception);
        return ResponseEntity.status(errorCode.getStatus())
                .header("X-Error-Id", errorResponse.getCorrelationId())
                .body(errorResponse);
    }

    private void log(ErrorCode errorCode, String method, String path, String correlationId, Exception exception) {
        String message = String.format("[API_ERROR] correlationId=%s code=%s method=%s path=%s exceptionType=%s",
                correlationId, errorCode.getCode(), method, path, exception.getClass().getSimpleName());

        if (errorCode.getStatus().is5xxServerError()) {
            log.error(message);
            return;
        }

        log.warn(message);
    }
}
