package bhoon.sugang_helper.common.security.exception;

import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.api.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding("UTF-8");

        ErrorCode errorCode = accessDeniedException instanceof CsrfException ? ErrorCode.CSRF_INVALID : ErrorCode.FORBIDDEN;
        ErrorResponse errorResponse = ErrorResponse.of(errorCode, request.getRequestURI());
        response.setHeader("X-Error-Id", errorResponse.getCorrelationId());
        logAccessDenied(errorResponse, request.getMethod(), accessDeniedException);
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }

    private void logAccessDenied(ErrorResponse error, String method, AccessDeniedException failure) {
        LoggingEventBuilder entry = log.atWarn().addKeyValue("event", "access_denied")
                .addKeyValue("errorCode", error.getCode())
                .addKeyValue("method", method)
                .addKeyValue("path", error.getPath())
                .addKeyValue("exceptionType", failure.getClass().getSimpleName());
        if (MDC.get("correlationId") == null) {
            entry.addKeyValue("correlationId", error.getCorrelationId());
        }
        entry.log("[ACCESS_DENIED] 접근 권한 없음.");
    }
}
