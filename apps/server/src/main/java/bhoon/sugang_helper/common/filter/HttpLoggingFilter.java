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

    private static final String COMPLETED_OUTCOME = "completed";
    private static final String ELAPSED_MS_KEY = "elapsedMs";
    private static final String EVENT_KEY = "event";
    private static final String METHOD_KEY = "method";
    private static final String PATH_KEY = "path";
    private static final String QUERY_PRESENT_KEY = "queryPresent";

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
                .addKeyValue(EVENT_KEY, "http_failure").addKeyValue(METHOD_KEY, method)
                .addKeyValue(PATH_KEY, uri).addKeyValue(QUERY_PRESENT_KEY, queryPresent)
                .addKeyValue("responseStatusAtExit", status).addKeyValue(ELAPSED_MS_KEY, tookMs)
                .addKeyValue("exceptionType", failureType).addKeyValue(COMPLETED_OUTCOME, false)
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
                .addKeyValue(EVENT_KEY, "http_request").addKeyValue(METHOD_KEY, method)
                .addKeyValue(PATH_KEY, uri).addKeyValue(QUERY_PRESENT_KEY, queryPresent)
                .addKeyValue("status", resolvedStatus).addKeyValue(ELAPSED_MS_KEY, tookMs)
                .addKeyValue(COMPLETED_OUTCOME, true)
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
