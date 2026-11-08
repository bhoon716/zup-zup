package bhoon.sugang_helper.common.filter;

import bhoon.sugang_helper.common.api.security.SensitiveDataRedactor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.spi.LoggingEventBuilder;
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
            logEscapingFailure(method, uri, queryPresent, status, tookMs, errorAlreadyLogged, failureType);
            return;
        }
        logCompletedRequest(method, uri, queryPresent, status, tookMs, errorAlreadyLogged);
    }

    private void logEscapingFailure(String method, String uri, boolean queryPresent, int status, long tookMs,
                                    boolean errorAlreadyLogged, String failureType) {
        String message = "[HTTP] 요청 처리 중 예외 전파. method={} path={} queryPresent={} "
                + "responseStatusAtExit={} elapsedMs={} exceptionType={} completed=false";
        failureLogEntry(errorAlreadyLogged)
                .addKeyValue("event", "http_failure").addKeyValue("method", method)
                .addKeyValue("path", uri).addKeyValue("queryPresent", queryPresent)
                .addKeyValue("responseStatusAtExit", status).addKeyValue("elapsedMs", tookMs)
                .addKeyValue("exceptionType", failureType).addKeyValue("completed", false)
                .log(message, method, uri, queryPresent, status, tookMs, failureType);
    }

    private LoggingEventBuilder failureLogEntry(boolean errorAlreadyLogged) {
        if (errorAlreadyLogged) {
            return log.atDebug();
        }
        return log.atError();
    }

    private void logCompletedRequest(String method, String uri, boolean queryPresent, int status, long tookMs,
                                     boolean errorAlreadyLogged) {
        int resolvedStatus = status;
        if (resolvedStatus == 0) {
            resolvedStatus = HttpServletResponse.SC_OK;
        }
        String message = "[HTTP] 요청 처리 결과. method={} path={} queryPresent={} status={} elapsedMs={}";
        completedRequestLogEntry(uri, resolvedStatus, errorAlreadyLogged)
                .addKeyValue("event", "http_request").addKeyValue("method", method)
                .addKeyValue("path", uri).addKeyValue("queryPresent", queryPresent)
                .addKeyValue("status", resolvedStatus).addKeyValue("elapsedMs", tookMs)
                .addKeyValue("completed", true)
                .log(message, method, uri, queryPresent, resolvedStatus, tookMs);
    }

    private LoggingEventBuilder completedRequestLogEntry(String uri, int status, boolean errorAlreadyLogged) {
        if (errorAlreadyLogged || uri.equals("/health") || status < 400) {
            return log.atDebug();
        }
        if (status >= 500) {
            return log.atError();
        }
        return log.atWarn();
    }
}
