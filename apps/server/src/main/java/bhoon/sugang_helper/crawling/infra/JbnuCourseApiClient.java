package bhoon.sugang_helper.crawling.infra;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.security.util.SensitiveDataRedactor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.SocketTimeoutException;
import java.net.ConnectException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JbnuCourseApiClient {

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

    /**
     * 하위 호환을 위해 메서드명은 유지하지만, 반환값은 JUMP JSON입니다.
     */
    public String fetchCourseDataXml(String year, String semester) {
        try (InputStream responseStream = fetchCourseDataStream(year, semester)) {
            return new String(responseStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.CRAWLER_CONNECTION_ERROR);
        }
    }

    public InputStream fetchCourseDataStream(String year, String semester) {
        int retryCount = 0;

        while (true) {
            long startedAt = System.nanoTime();
            try {
                Map<String, String> cookies = fetchSessionCookies();
                if (cookies.isEmpty()) {
                    throw new IOException("JUMP session cookies are empty");
                }

                Connection.Response response = Jsoup.connect(apiUrl)
                        .cookies(cookies)
                        .header("Accept", "application/json, */*")
                        .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                        .header("Cache-Control", "no-cache")
                        .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                        .header("Origin", originOf(apiUrl))
                        .header("Referer", bootstrapUrl)
                        .header("Pragma", "no-cache")
                        .header("User-Agent", "Mozilla/5.0 (compatible; jbnu-sugang-helper-crawler)")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .header("xb_req_type", "enc")
                        .requestBody(JbnuJumpRequestEncoder.encode(buildRequest(year, semester)))
                        .timeout(timeoutMs)
                        .maxBodySize(maximumResponseBytes)
                        .method(Connection.Method.POST)
                        .ignoreContentType(true)
                        .execute();
                if (response.statusCode() >= 400) {
                    throw new IOException("JUMP responded with HTTP " + response.statusCode());
                }
                recordUpstreamLatency(startedAt);
                return new BoundedInputStream(response.bodyStream(), maximumResponseBytes);
            } catch (Exception e) {
                retryCount++;
                recordUpstreamLatency(startedAt);
                if (!isTransientFailure(e)) {
                    log.error("[API Client] Non-transient JUMP request failure. yy={}, semester={}, exceptionType={}",
                            year, semester, SensitiveDataRedactor.exceptionType(e));
                    throw new CustomException(ErrorCode.CRAWLER_CONNECTION_ERROR);
                }
                log.warn("[API Client] JUMP course request failed. attempt={}/{}, yy={}, semester={}, exceptionType={}",
                        retryCount, maxRetries + 1, year, semester, SensitiveDataRedactor.exceptionType(e));
                if (retryCount > maxRetries) {
                    throw new CustomException(ErrorCode.CRAWLER_CONNECTION_ERROR);
                }
                waitBeforeRetry(retryCount);
            }
        }
    }

    static String toJumpSemesterCode(String semester) {
        return switch (semester) {
            case "U211600010" -> "SUSR016.010";
            case "U211600020" -> "SUSR016.020";
            case "U211600015" -> "SUSR016.015";
            case "U211600025" -> "SUSR016.025";
            case "U211600016" -> "SUSR016.016";
            case "U211600026" -> "SUSR016.026";
            case "U211600009" -> "SUSR016.009";
            case "U211600008" -> "SUSR016.008";
            default -> semester;
        };
    }

    private Map<String, String> buildRequest(String year, String semester) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("strYrsa", year);
        values.put("strSemstrCd", toJumpSemesterCode(semester));
        values.put("strOpnltDivCd", "");
        values.put("strEngLctrYn", "");
        values.put("strCertDivCd", certDivision == null || certDivision.isBlank() ? DEFAULT_CERT_DIVISION : certDivision);
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

    private Map<String, String> fetchSessionCookies() throws IOException {
        Connection.Response response = Jsoup.connect(bootstrapUrl)
                .header("Accept", "text/html, */*")
                .header("User-Agent", "Mozilla/5.0 (compatible; jbnu-sugang-helper-crawler)")
                .timeout(timeoutMs)
                .method(Connection.Method.GET)
                .execute();
        return response.cookies();
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
        if (throwable instanceof SocketTimeoutException || throwable instanceof ConnectException
                || throwable instanceof IOException || throwable instanceof java.util.concurrent.TimeoutException) {
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
            timer.record(System.nanoTime() - startedAt, java.util.concurrent.TimeUnit.NANOSECONDS);
        }
    }

    private void waitBeforeRetry(int retryCount) {
        try {
            long waitTime = (long) retryWaitMs * retryCount;
            log.info("[API Client] Waiting {}ms before retry...", waitTime);
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "재시도 대기 중 프로세스가 중단되었습니다.");
        }
    }
}
