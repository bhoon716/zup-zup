package bhoon.sugang_helper.common.filter;

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

        String uri = request.getRequestURI();
        String query = request.getQueryString();
        if (query != null) {
            uri += "?" + query;
        }
        String method = request.getMethod();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long tookMs = System.currentTimeMillis() - start;
            int status = response.getStatus();
            logByStatus(method, uri, status, tookMs);
        }
    }

    private void logByStatus(String method, String uri, int status, long tookMs) {
        status = status == 0 ? HttpServletResponse.SC_OK : status;
        if (status >= 500) {
            log.error("[HTTP] {} {} -> {} ({} ms)", method, uri, status, tookMs);
        } else if (status >= 400) {
            log.warn("[HTTP] {} {} -> {} ({} ms)", method, uri, status, tookMs);
        } else {
            log.info("[HTTP] {} {} -> {} ({} ms)", method, uri, status, tookMs);
        }
    }
}
