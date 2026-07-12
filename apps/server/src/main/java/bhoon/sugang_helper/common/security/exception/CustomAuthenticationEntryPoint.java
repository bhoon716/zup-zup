package bhoon.sugang_helper.common.security.exception;

import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.INVALID_TOKEN, request.getRequestURI());
        response.setHeader("X-Error-Id", errorResponse.getCorrelationId());
        log.warn("[AUTHENTICATION_FAILED] correlationId={} method={} path={} exceptionType={}",
                errorResponse.getCorrelationId(), request.getMethod(), errorResponse.getPath(),
                authException.getClass().getSimpleName());
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
