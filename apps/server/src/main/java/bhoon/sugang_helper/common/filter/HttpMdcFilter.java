package bhoon.sugang_helper.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HttpMdcFilter extends OncePerRequestFilter {

    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
    public static final String MDC_KEY_CORRELATION_ID = "correlationId";
    public static final int MAX_CORRELATION_ID_LENGTH = 64;

    private static final Pattern CORRELATION_ID_PATTERN =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");

    @Override
    @SuppressWarnings("NullableProblems")
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String correlationId = resolveCorrelationId(request.getHeader(HEADER_CORRELATION_ID));

        MDC.put(MDC_KEY_CORRELATION_ID, correlationId);
        response.setHeader(HEADER_CORRELATION_ID, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private static String resolveCorrelationId(String requestedCorrelationId) {
        if (isValidCorrelationId(requestedCorrelationId)) {
            return requestedCorrelationId;
        }
        return UUID.randomUUID().toString();
    }

    private static boolean isValidCorrelationId(String correlationId) {
        return correlationId != null
                && !correlationId.isBlank()
                && correlationId.length() <= MAX_CORRELATION_ID_LENGTH
                && CORRELATION_ID_PATTERN.matcher(correlationId).matches();
    }
}
