package bhoon.sugang_helper.academic.crawling.client;

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
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@Slf4j
public class JbnuCourseApiClient implements CourseApiClient {

    private static final String DEFAULT_BOOTSTRAP_URL = "https://jump.jbnu.ac.kr/link?div=sucrPlan";
    private static final String DEFAULT_CERT_DIVISION = "6";
    private static final String KOREAN_LANGUAGE_CODE = "CCMN101.KOR";

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
        if (requestedDivision != null && !requestedDivision.isBlank()) {
            return requestedDivision;
        }
        if (this.certDivision != null && !this.certDivision.isBlank()) {
            return this.certDivision;
        }
        return DEFAULT_CERT_DIVISION;
    }

    private HttpClient createHttpClient(CookieManager cookieManager) {
        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .cookieHandler(cookieManager)
                .followRedirects(HttpClient.Redirect.ALWAYS);
        if (timeoutMs > 0) {
            httpClientBuilder.connectTimeout(Duration.ofMillis(timeoutMs));
        }

        return httpClientBuilder.build();
    }

    private byte[] requestCourseData(RestClient restClient, String year, String semester,
                                      String targetCertDivision) {
        try {
            return restClient.post()
                    .uri(apiUrl)
                    .header("Accept", "application/json, */*")
                    .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                    .header("Cache-Control", "no-cache")
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .header("Origin", originOf(apiUrl))
                    .header("Referer", bootstrapUrl)
                    .header("Pragma", "no-cache")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("xb_req_type", "enc")
                    .body(JbnuJumpRequestEncoder.encode(buildRequest(year, semester, targetCertDivision)))
                    .exchange((request, response) -> readResponse(response));
        } catch (Exception exception) {
            throw new CrawlerUpstreamException(CrawlerFailureStage.COURSE_API,
                    upstreamStatusCode(exception), isTransientFailure(exception), exception);
        }
    }

    private RestClient createRestClient(HttpClient httpClient) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        if (timeoutMs > 0) {
            requestFactory.setReadTimeout(timeoutMs);
        }
        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (compatible; jbnu-sugang-helper-crawler)")
                .defaultHeader(HttpHeaders.ACCEPT_ENCODING, "gzip")
                .build();
    }

    byte[] readResponse(ClientHttpResponse response) throws IOException {
        if (response.getStatusCode().isError()) {

            throw new RestClientResponseException("JUMP responded with HTTP " + response.getStatusCode().value(),
                    response.getStatusCode().value(), response.getStatusText(), null, new byte[0],
                    StandardCharsets.UTF_8);
        }
        InputStream body = response.getBody();
        if ("gzip".equalsIgnoreCase(response.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING))) {
            body = new GZIPInputStream(body);
        }

        if (maximumResponseBytes > 0) {
            body = new BoundedInputStream(body, maximumResponseBytes);
        }
        try (InputStream input = body) {
            return input.readAllBytes();
        }
    }

    private void handleRequestError(Exception e, int retryCount, String year, String semester, String targetCertDivision) {
        boolean retryable = isTransientFailure(e);
        logRequestFailure(e, retryCount, year, semester, targetCertDivision, retryable);
        if (!retryable || retryCount > maxRetries) {
            throw connectionFailure(e);
        }
    }

    private void logRequestFailure(Exception e, int retryCount, String year, String semester,
                                   String targetCertDivision, boolean retryable) {
        CrawlerFailureStage stage = failureStage(e);
        String status = "UNKNOWN";
        if (e instanceof CrawlerUpstreamException upstream) {
            status = statusLabel(upstream.upstreamStatus());
        }
        int maximumAttempts = maxRetries + 1;
        boolean exhausted = retryCount > maxRetries;
        String result = requestFailureResult(retryable, exhausted);
        String exceptionType = SensitiveDataRedactor.exceptionType(
                CrawlerUpstreamException.rootCause(e));
        Level level = Level.ERROR;
        if (retryable) {
            level = Level.WARN;
        }
        log.atLevel(level).addKeyValue("event", "upstream_request_failed")
                .addKeyValue("year", year)
                .addKeyValue("semester", semester)
                .addKeyValue("certDiv", targetCertDivision)
                .addKeyValue("stage", stage)
                .addKeyValue("attempt", retryCount)
                .addKeyValue("maximumAttempts", maximumAttempts)
                .addKeyValue("upstreamStatus", status)
                .addKeyValue("exceptionType", exceptionType)
                .addKeyValue("retryable", retryable)
                .addKeyValue("result", result)
                .log("[Crawler] 상위 강의 API 요청 실패.");
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

    static String toJumpSemesterCode(String semester) {
        if (semester.equals("U211600010")) {
            return "SUSR016.010";
        }
        if (semester.equals("U211600020")) {
            return "SUSR016.020";
        }
        if (semester.equals("U211600015")) {
            return "SUSR016.015";
        }
        if (semester.equals("U211600025")) {
            return "SUSR016.025";
        }
        if (semester.equals("U211600016")) {
            return "SUSR016.016";
        }
        if (semester.equals("U211600026")) {
            return "SUSR016.026";
        }
        if (semester.equals("U211600009")) {
            return "SUSR016.009";
        }
        if (semester.equals("U211600008")) {
            return "SUSR016.008";
        }
        return semester;
    }

    private Map<String, String> buildRequest(String year, String semester, String targetCertDivision) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("strYrsa", year);
        values.put("strSemstrCd", toJumpSemesterCode(semester));
        values.put("strOpnltDivCd", "");
        values.put("strEngLctrYn", "");
        values.put("strCertDivCd", targetCertDivision);
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

    private void fetchSessionCookies(RestClient restClient, CookieManager cookieManager) {
        try {
            restClient.get()
                    .uri(bootstrapUrl)
                    .header("Accept", "text/html, */*")
                    .exchange((request, response) -> readResponse(response));
            if (sessionCookiesExpired(cookieManager)) {
                throw new IOException("JUMP session cookies are empty");
            }
        } catch (Exception exception) {
            throw new CrawlerUpstreamException(CrawlerFailureStage.JUMP_BOOTSTRAP,
                    upstreamStatusCode(exception), isTransientFailure(exception), exception);
        }
    }

    private boolean sessionCookiesExpired(CookieManager cookieManager) {
        return cookieManager.getCookieStore().getCookies().stream().allMatch(HttpCookie::hasExpired);
    }

    private class JumpSession implements Session {

        private final CookieManager cookieManager = new CookieManager();
        private HttpClient httpClient;
        private RestClient restClient;

        @Override
        public InputStream fetchCourseDataStream(String year, String semester, String certDivision) {
            String targetCertDivision = resolveCertDivision(certDivision);
            int retryCount = 0;

            while (true) {
                long startedAt = System.nanoTime();
                try {
                    byte[] bodyBytes = requestCourseData(authenticatedClient(), year, semester, targetCertDivision);
                    recordUpstreamLatency(startedAt);
                    return new BoundedInputStream(new ByteArrayInputStream(bodyBytes), maximumResponseBytes);
                } catch (Exception e) {
                    close();
                    recordUpstreamLatency(startedAt);
                    retryCount++;
                    handleRequestError(e, retryCount, year, semester, targetCertDivision);
                    waitBeforeRetry(retryCount, year, semester, targetCertDivision, e);
                }
            }
        }

        private RestClient authenticatedClient() {
            if (httpClient == null || httpClient.isTerminated()) {
                httpClient = createHttpClient(cookieManager);
                restClient = createRestClient(httpClient);
            }
            if (sessionCookiesExpired(cookieManager)) {
                fetchSessionCookies(restClient, cookieManager);
            }
            return restClient;
        }

        @Override
        public void close() {
            if (httpClient == null) {
                return;
            }
            httpClient.close();
            cookieManager.getCookieStore().removeAll();
        }
    }

    private String originOf(String url) {
        try {
            URI uri = new URI(url);
            return uri.getScheme() + "://" + uri.getAuthority();
        } catch (URISyntaxException e) {
            return "https://jump.jbnu.ac.kr";
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

    private void recordUpstreamLatency(long startedAt) {
        if (meterRegistry == null) {
            return;
        }
        Timer timer = meterRegistry.timer("crawler.upstream.latency");
        if (timer != null) {
            timer.record(System.nanoTime() - startedAt, TimeUnit.NANOSECONDS);
        }
    }

    private void waitBeforeRetry(int retryCount, String year, String semester, String targetCertDivision,
                                 Exception failure) {
        try {
            long waitTime = (long) retryWaitMs * retryCount;
            logRetryWait(retryCount, year, semester, targetCertDivision, failure, waitTime);
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "재시도 대기 중 프로세스가 중단되었습니다.");
        }
    }

    private void logRetryWait(int retryCount, String year, String semester, String targetCertDivision,
                              Exception failure, long waitTime) {
        CrawlerFailureStage stage = failureStage(failure);
        log.atDebug().addKeyValue("event", "upstream_retry_wait")
                .addKeyValue("year", year)
                .addKeyValue("semester", semester)
                .addKeyValue("certDiv", targetCertDivision)
                .addKeyValue("stage", stage)
                .addKeyValue("attempt", retryCount)
                .addKeyValue("waitMs", waitTime)
                .addKeyValue("result", "waiting")
                .log("[Crawler] 상위 강의 API 재시도 대기.");
    }

    private CustomException connectionFailure(Exception cause) {
        if (cause instanceof CrawlerUpstreamException upstream) {
            return upstream;
        }
        CustomException exception = new CustomException(ErrorCode.CRAWLER_CONNECTION_ERROR);
        exception.initCause(cause);
        return exception;
    }

    private Integer upstreamStatusCode(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof RestClientResponseException statusException) {
                return statusException.getStatusCode().value();
            }
            current = current.getCause();
        }
        return null;
    }

    private String statusLabel(Integer status) {
        if (status == null) {
            return "UNKNOWN";
        }
        return status.toString();
    }
}
