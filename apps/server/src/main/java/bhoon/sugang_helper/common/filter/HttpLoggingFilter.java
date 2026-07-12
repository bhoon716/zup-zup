package bhoon.sugang_helper.common.filter;

import bhoon.sugang_helper.common.security.util.SensitiveDataRedactor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class HttpLoggingFilter extends OncePerRequestFilter {

    @Override
    @SuppressWarnings("NullableProblems")
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        long start = System.currentTimeMillis();

        String uri = SensitiveDataRedactor.redactPath(request.getRequestURI());
        if (uri.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean queryPresent = request.getQueryString() != null;
        String method = request.getMethod();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long tookMs = System.currentTimeMillis() - start;
            int status = response.getStatus();
            logByStatus(method, uri, queryPresent, status, tookMs);
        }
    }

    private void logByStatus(String method, String uri, boolean queryPresent, int status, long tookMs) {
        int resolvedStatus = status == 0 ? HttpServletResponse.SC_OK : status;
        if (resolvedStatus >= 500) {
            log.error("[HTTP] {} {} queryPresent={} -> {} ({} ms)", method, uri, queryPresent, resolvedStatus, tookMs);
        } else if (resolvedStatus >= 400) {
            log.warn("[HTTP] {} {} queryPresent={} -> {} ({} ms)", method, uri, queryPresent, resolvedStatus, tookMs);
        } else {
            log.info("[HTTP] {} {} queryPresent={} -> {} ({} ms)", method, uri, queryPresent, resolvedStatus, tookMs);
        }
    }
}
