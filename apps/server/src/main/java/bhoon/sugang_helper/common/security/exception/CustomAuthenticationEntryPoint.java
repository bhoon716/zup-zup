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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String CORRELATION_ID_KEY = "correlationId";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.AUTHENTICATION_REQUIRED, request.getRequestURI());
        response.setHeader("X-Error-Id", errorResponse.getCorrelationId());
        logAuthenticationFailed(errorResponse, request.getMethod(), authException);
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }

    private void logAuthenticationFailed(ErrorResponse error, String method, AuthenticationException failure) {
        LoggingEventBuilder entry = log.atWarn().addKeyValue("event", "authentication_failed")
                .addKeyValue("errorCode", error.getCode())
                .addKeyValue("method", method)
                .addKeyValue("path", error.getPath())
                .addKeyValue("exceptionType", failure.getClass().getSimpleName());
        if (MDC.get(CORRELATION_ID_KEY) == null) {
            entry.addKeyValue(CORRELATION_ID_KEY, error.getCorrelationId());
        }
        entry.log("[AUTHENTICATION_FAILED] 인증 실패.");
    }
}
