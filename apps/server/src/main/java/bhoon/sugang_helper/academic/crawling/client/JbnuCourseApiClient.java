package bhoon.sugang_helper.academic.crawling.client;

import bhoon.sugang_helper.academic.course.enums.SemesterType;
import bhoon.sugang_helper.academic.crawling.enums.CrawlerFailureStage;
import bhoon.sugang_helper.academic.crawling.exception.CrawlerUpstreamException;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.api.security.SensitiveDataRedactor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.GZIPInputStream;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@Slf4j
public class JbnuCourseApiClient implements CourseApiClient {

    private static final String DEFAULT_BOOTSTRAP_URL = "https://jump.jbnu.ac.kr/link?div=sucrPlan";
    private static final String DEFAULT_CERT_DIVISION = "6";
    private static final String KOREAN_LANGUAGE_CODE = "CCMN101.KOR";
    private static final String GZIP_ENCODING = "gzip";
    private static final String NO_CACHE = "no-cache";

    private static final String ATTEMPT_KEY = "attempt";
    private static final String CERT_DIVISION_KEY = "certDiv";
    private static final String EVENT_KEY = "event";
    private static final String RESULT_KEY = "result";
    private static final String SEMESTER_KEY = "semester";
    private static final String STAGE_KEY = "stage";
    private static final String YEAR_KEY = "year";
    private static final String UNKNOWN_STATUS = "UNKNOWN";

    @Value("${jbnu.api.url}")
    private String apiUrl;

    @Value("${jbnu.api.bootstrap-url:" + DEFAULT_BOOTSTRAP_URL + "}")
    private String bootstrapUrl;

    @Value("${jbnu.api.cert-division:" + DEFAULT_CERT_DIVISION + "}")
    private String certDivision;

    @Value("${jbnu.api.timeout-ms}")
    private int timeoutMs;

    @Value("${jbnu.api.max-retries}")
    private int maxRetries;

    @Value("${jbnu.api.retry-wait-ms:1000}")
    private int retryWaitMs;

    @Value("${jbnu.api.max-response-bytes:10485760}")
    private int maximumResponseBytes;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    public String fetchCourseData(String year, String semester) {
        try (InputStream responseStream = fetchCourseDataStream(year, semester)) {
            return new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.CRAWLER_CONNECTION_ERROR);
        }
    }

    public InputStream fetchCourseDataStream(String year, String semester) {
        return fetchCourseDataStream(year, semester, this.certDivision);
    }

    @Override
    public InputStream fetchCourseDataStream(String year, String semester, String certDivision) {
        try (Session session = openSession()) {
            return session.fetchCourseDataStream(year, semester, certDivision);
        }
    }

    @Override
    public Session openSession() {
        return new JumpSession();
    }

    private String resolveCertDivision(String requestedDivision) {
        if (StringUtils.hasText(requestedDivision)) {
            return requestedDivision;
        }
        if (StringUtils.hasText(this.certDivision)) {
            return this.certDivision;
        }
        return DEFAULT_CERT_DIVISION;
    }

    private byte[] requestCourseData(RestClient restClient, CourseRequest courseRequest) {
        try {
            return restClient.post()
                    .uri(apiUrl)
                    .header(HttpHeaders.ACCEPT, "application/json, */*")
                    .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                    .header(HttpHeaders.CACHE_CONTROL, NO_CACHE)
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .header("Origin", originOf(apiUrl))
                    .header("Referer", bootstrapUrl)
                    .header(HttpHeaders.PRAGMA, NO_CACHE)
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("xb_req_type", "enc")
                    .body(JbnuJumpRequestEncoder.encode(buildRequestFields(courseRequest)))
                    .exchange((request, response) -> readResponse(response));
        } catch (Exception exception) {
            throw upstreamFailure(CrawlerFailureStage.COURSE_API, exception);
        }
    }

    private Map<String, String> buildRequestFields(CourseRequest request) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("strYrsa", request.year());
        values.put("strSemstrCd", SemesterType.toJumpCode(request.semester()));
        values.put("strOpnltDivCd", "");
        values.put("strEngLctrYn", "");
        values.put("strCertDivCd", request.certDivision());
        values.put("strSbjctNm", "");
        values.put("strProfNo", "");
        values.put("strProfNm", "");
        values.put("strMngClgCd", "");
        values.put("strMngScsbjtCd", "");
        values.put("strScyrDivCd", "");
        values.put("strRlmDivCd", "");
        values.put("strCultDivCd", "");
        values.put("strMtcltnYr", "");
        values.put("strGdMngClgCd", "");
        values.put("strGdMngScsbjtCd", "");
        values.put("strLanDivcd", KOREAN_LANGUAGE_CODE);
        values.put("strLang", "ko");
        return values;
    }

    private String originOf(String url) {
        try {
            URI uri = new URI(url);
            return uri.getScheme() + "://" + uri.getAuthority();
        } catch (URISyntaxException e) {
            return "https://jump.jbnu.ac.kr";
        }
    }

    byte[] readResponse(ClientHttpResponse response) throws IOException {
        HttpStatusCode status = response.getStatusCode();
        if (status.isError()) {
            throw new RestClientResponseException("JUMP 요청에 실패했습니다. HTTP 상태 코드: " + status.value(),
                    status.value(), response.getStatusText(), null, new byte[0], StandardCharsets.UTF_8);
        }

        InputStream body = response.getBody();
        if (GZIP_ENCODING.equalsIgnoreCase(response.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING))) {
            body = new GZIPInputStream(body);
        }

        if (maximumResponseBytes > 0) {
            body = new BoundedInputStream(body, maximumResponseBytes);
        }
        try (InputStream input = body) {
            return input.readAllBytes();
        }
    }

    private void retryOrThrow(CourseRequest request, int attempt, Exception failure) {
        boolean retryable = isTransientFailure(failure);
        boolean exhausted = attempt > maxRetries;
        logRequestFailure(request, attempt, failure, retryable, exhausted);
        if (!retryable || exhausted) {
            throw connectionFailure(failure);
        }
        waitBeforeRetry(request, attempt, failure);
    }

    private void waitBeforeRetry(CourseRequest request, int attempt, Exception failure) {
        try {
            long waitTime = (long) retryWaitMs * attempt;
            logRetryWait(request, attempt, failure, waitTime);
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "재시도 대기 중 프로세스가 중단되었습니다.");
        }
    }

    static boolean isTransientFailure(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        if (throwable instanceof CrawlerUpstreamException upstream) {
            return upstream.retryable();
        }
        if (throwable instanceof SocketTimeoutException || throwable instanceof ConnectException
                || throwable instanceof IOException || throwable instanceof TimeoutException
                || throwable instanceof RestClientResponseException) {
            return true;
        }
        return isTransientFailure(throwable.getCause());
    }

    private static CrawlerUpstreamException upstreamFailure(CrawlerFailureStage stage, Exception failure) {
        return new CrawlerUpstreamException(stage, upstreamStatusCode(failure), isTransientFailure(failure), failure);
    }

    private static Integer upstreamStatusCode(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof RestClientResponseException statusException) {
                return statusException.getStatusCode().value();
            }
            current = current.getCause();
        }
        return null;
    }

    private CustomException connectionFailure(Exception cause) {
        if (cause instanceof CrawlerUpstreamException upstream) {
            return upstream;
        }
        CustomException exception = new CustomException(ErrorCode.CRAWLER_CONNECTION_ERROR);
        exception.initCause(cause);
        return exception;
    }

    private void recordUpstreamLatency(long startedAt) {
        if (meterRegistry == null) {
            return;
        }
        Timer timer = meterRegistry.timer("crawler.upstream.latency");
        if (timer != null) {
            timer.record(System.nanoTime() - startedAt, TimeUnit.NANOSECONDS);
        }
    }

    private void logRequestFailure(CourseRequest request, int attempt, Exception failure,
                                   boolean retryable, boolean exhausted) {
        CrawlerFailureStage stage = failureStage(failure);
        String status = UNKNOWN_STATUS;
        if (failure instanceof CrawlerUpstreamException upstream) {
            status = statusLabel(upstream.upstreamStatus());
        }
        String result = requestFailureResult(retryable, exhausted);
        String exceptionType = SensitiveDataRedactor.exceptionType(CrawlerUpstreamException.rootCause(failure));
        Level level = retryable ? Level.WARN : Level.ERROR;
        log.atLevel(level).addKeyValue(EVENT_KEY, "upstream_request_failed")
                .addKeyValue(YEAR_KEY, request.year())
                .addKeyValue(SEMESTER_KEY, request.semester())
                .addKeyValue(CERT_DIVISION_KEY, request.certDivision())
                .addKeyValue(STAGE_KEY, stage)
                .addKeyValue(ATTEMPT_KEY, attempt)
                .addKeyValue("maximumAttempts", maxRetries + 1)
                .addKeyValue("upstreamStatus", status)
                .addKeyValue("exceptionType", exceptionType)
                .addKeyValue("retryable", retryable)
                .addKeyValue(RESULT_KEY, result)
                .log("[Crawler] 상위 강의 API 요청 실패.");
    }

    private void logRetryWait(CourseRequest request, int attempt, Exception failure, long waitTime) {
        CrawlerFailureStage stage = failureStage(failure);
        log.atDebug().addKeyValue(EVENT_KEY, "upstream_retry_wait")
                .addKeyValue(YEAR_KEY, request.year())
                .addKeyValue(SEMESTER_KEY, request.semester())
                .addKeyValue(CERT_DIVISION_KEY, request.certDivision())
                .addKeyValue(STAGE_KEY, stage)
                .addKeyValue(ATTEMPT_KEY, attempt)
                .addKeyValue("waitMs", waitTime)
                .addKeyValue(RESULT_KEY, "waiting")
                .log("[Crawler] 상위 강의 API 재시도 대기.");
    }

    private CrawlerFailureStage failureStage(Exception failure) {
        if (failure instanceof CrawlerUpstreamException upstream) {
            return upstream.stage();
        }
        return CrawlerFailureStage.FETCH_PARSE;
    }

    private String requestFailureResult(boolean retryable, boolean exhausted) {
        if (!retryable) {
            return "failed";
        }
        if (exhausted) {
            return "exhausted";
        }
        return "retrying";
    }

    private String statusLabel(Integer status) {
        if (status == null) {
            return UNKNOWN_STATUS;
        }
        return status.toString();
    }

    private class JumpSession implements Session {

        private final CookieManager cookieManager = new CookieManager();
        private HttpClient httpClient;
        private RestClient restClient;

        @Override
        public InputStream fetchCourseDataStream(String year, String semester, String certDivision) {
            CourseRequest request = new CourseRequest(year, semester, resolveCertDivision(certDivision));
            int attempt = 1;

            while (true) {
                long startedAt = System.nanoTime();
                try {
                    byte[] bodyBytes = requestCourseData(authenticatedClient(), request);
                    recordUpstreamLatency(startedAt);
                    return new BoundedInputStream(new ByteArrayInputStream(bodyBytes), maximumResponseBytes);
                } catch (Exception failure) {
                    close();
                    recordUpstreamLatency(startedAt);
                    retryOrThrow(request, attempt, failure);
                    attempt++;
                }
            }
        }

        @Override
        public void close() {
            if (httpClient == null) {
                return;
            }
            httpClient.close();
            cookieManager.getCookieStore().removeAll();
        }

        private RestClient authenticatedClient() {
            if (httpClient == null || httpClient.isTerminated()) {
                httpClient = createHttpClient();
                restClient = createRestClient(httpClient);
            }
            if (needsSessionCookies()) {
                fetchSessionCookies();
            }
            return restClient;
        }

        private HttpClient createHttpClient() {
            HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .cookieHandler(cookieManager)
                    .followRedirects(HttpClient.Redirect.ALWAYS);
            if (timeoutMs > 0) {
                httpClientBuilder.connectTimeout(Duration.ofMillis(timeoutMs));
            }

            return httpClientBuilder.build();
        }

        private RestClient createRestClient(HttpClient httpClient) {
            JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
            if (timeoutMs > 0) {
                requestFactory.setReadTimeout(timeoutMs);
            }
            return RestClient.builder()
                    .requestFactory(requestFactory)
                    .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (compatible; jbnu-sugang-helper-crawler)")
                    .defaultHeader(HttpHeaders.ACCEPT_ENCODING, GZIP_ENCODING)
                    .build();
        }

        private void fetchSessionCookies() {
            try {
                restClient.get()
                        .uri(bootstrapUrl)
                        .header(HttpHeaders.ACCEPT, "text/html, */*")
                        .exchange((request, response) -> readResponse(response));
                if (needsSessionCookies()) {
                    throw new IOException("JUMP 세션 쿠키를 받지 못했습니다.");
                }
            } catch (Exception exception) {
                throw upstreamFailure(CrawlerFailureStage.JUMP_BOOTSTRAP, exception);
            }
        }

        private boolean needsSessionCookies() {
            return cookieManager.getCookieStore().getCookies().stream().allMatch(HttpCookie::hasExpired);
        }
    }

    private record CourseRequest(String year, String semester, String certDivision) {
    }
}
