package bhoon.sugang_helper.common.error;

import bhoon.sugang_helper.common.response.ErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final long STACK_TRACE_LOG_INTERVAL_NANOS = TimeUnit.MINUTES.toNanos(1);
    private static final int MAX_STACK_TRACE_FINGERPRINTS = 256;
    private static final int MAX_EXCEPTION_CAUSE_DEPTH = 8;
    private final Map<String, Long> stackTraceLogTimes = new LinkedHashMap<>(16, 0.75f, true);

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(HttpServletRequest req, CustomException e) {
        return response(req, e.getErrorCode(), e);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(HttpServletRequest req, MethodArgumentNotValidException e) {
        return response(req, ErrorCode.INVALID_INPUT, e);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(HttpServletRequest req,
                                                                   HttpMessageNotReadableException e) {
        return response(req, ErrorCode.INVALID_INPUT, e);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(HttpServletRequest req,
                                                              MaxUploadSizeExceededException e) {
        return response(req, ErrorCode.MAX_FILE_UPLOAD_SIZE_EXCEEDED, e);
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
            boolean includeStackTrace = shouldLogStackTrace(errorCode, exception);
            String stackTraceState = " stackTraceIncluded=" + includeStackTrace;
            if (includeStackTrace) {
                log.error(message + stackTraceState, exception);
            } else {
                log.error(message + stackTraceState);
            }
            return;
        }

        log.warn(message);
    }

    private synchronized boolean shouldLogStackTrace(ErrorCode errorCode, Exception exception) {
        String fingerprint = exceptionFingerprint(errorCode, exception);
        long now = System.nanoTime();
        Long previous = stackTraceLogTimes.get(fingerprint);
        if (previous != null && now - previous < STACK_TRACE_LOG_INTERVAL_NANOS) {
            return false;
        }

        stackTraceLogTimes.put(fingerprint, now);
        if (stackTraceLogTimes.size() > MAX_STACK_TRACE_FINGERPRINTS) {
            Iterator<String> iterator = stackTraceLogTimes.keySet().iterator();
            iterator.next();
            iterator.remove();
        }
        return true;
    }

    private String exceptionFingerprint(ErrorCode errorCode, Exception exception) {
        StringBuilder fingerprint = new StringBuilder(errorCode.getCode());
        Throwable cause = exception;
        for (int depth = 0; cause != null && depth < MAX_EXCEPTION_CAUSE_DEPTH; depth++) {
            fingerprint.append(":").append(cause.getClass().getName());
            cause = cause.getCause();
        }
        return fingerprint.toString();
    }
}
