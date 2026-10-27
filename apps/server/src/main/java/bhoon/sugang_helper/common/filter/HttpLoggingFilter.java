package bhoon.sugang_helper.common.filter;

import bhoon.sugang_helper.common.security.util.SensitiveDataRedactor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
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

        long start = System.nanoTime();

        String uri = SensitiveDataRedactor.redactPath(request.getRequestURI());
        if (uri.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean queryPresent = request.getQueryString() != null;
        String method = request.getMethod();
        String failureType = null;

        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException | Error exception) {
            failureType = exception.getClass().getSimpleName();
            throw exception;
        } finally {
            long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            int status = response.getStatus();
            logByStatus(method, uri, queryPresent, status, tookMs,
                    response.getHeader("X-Error-Id") != null, failureType);
        }
    }

    private void logByStatus(String method, String uri, boolean queryPresent, int status, long tookMs,
                             boolean errorAlreadyLogged, String failureType) {
        if (failureType != null) {
            String failureMessage = "[HTTP] 요청 처리 중 예외 전파. method={} path={} queryPresent={} "
                    + "responseStatusAtExit={} elapsedMs={} exceptionType={} completed=false";
            if (errorAlreadyLogged) {
                log.debug(failureMessage, method, uri, queryPresent, status, tookMs, failureType);
            } else {
                log.error(failureMessage, method, uri, queryPresent, status, tookMs, failureType);
            }
            return;
        }
        int resolvedStatus = status == 0 ? HttpServletResponse.SC_OK : status;
        String message = "[HTTP] 요청 처리 결과. method={} path={} queryPresent={} status={} elapsedMs={}";
        if (errorAlreadyLogged || uri.equals("/health") || resolvedStatus < 400) {
            log.debug(message, method, uri, queryPresent, resolvedStatus, tookMs);
            return;
        }
        if (resolvedStatus >= 500) {
            log.error(message, method, uri, queryPresent, resolvedStatus, tookMs);
            return;
        }
        log.warn(message, method, uri, queryPresent, resolvedStatus, tookMs);
    }
}
