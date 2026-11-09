package bhoon.sugang_helper.common.alert;

import bhoon.sugang_helper.common.api.alerts.OperationalAlerts;
import bhoon.sugang_helper.common.api.alerts.SlackAlertProperties;
import bhoon.sugang_helper.common.api.alerts.SlackAlertCategory;
import bhoon.sugang_helper.common.api.security.SensitiveDataRedactor;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.LongSupplier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SlackAlertService implements OperationalAlerts {

    private static final int MAX_DEDUPLICATION_KEYS = 256;
    private static final DateTimeFormatter ALERT_TIME_FORMAT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Seoul"));
    private static final String CATEGORY_KEY = "category";
    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final String CRAWLER_RUN_ID_KEY = "crawlerRunId";
    private static final String UNSUPPORTED_CATEGORY = "Unsupported alert category: ";
    private static final String REJECTED_OUTCOME = "rejected";
    private final SlackAlertProperties properties;
    private final SlackWebhookClient webhookClient;
    private final long cooldownNanos;
    private final MeterRegistry registry;
    private final LongSupplier nanoTime;
    private final ExecutorService executor;
    private final Map<String, Long> lastAlertNanos = new LinkedHashMap<>();

    @Autowired
    public SlackAlertService(SlackAlertProperties properties, SlackWebhookClient webhookClient,
                             MeterRegistry registry) {
        this(properties, webhookClient, registry, System::nanoTime, createExecutor());
    }

    SlackAlertService(SlackAlertProperties properties, SlackWebhookClient webhookClient,
                      MeterRegistry registry, LongSupplier nanoTime, ExecutorService executor) {
        this.properties = properties;
        this.webhookClient = webhookClient;
        this.registry = registry;
        this.nanoTime = nanoTime;
        this.executor = executor;
        this.cooldownNanos = TimeUnit.SECONDS.toNanos(properties.cooldownSeconds());
        for (SlackAlertCategory category : SlackAlertCategory.values()) {
            for (String outcome : List.of("success", "failure", "suppressed", REJECTED_OUTCOME, "disabled")) {
                registry.counter("slack.alert.delivery", CATEGORY_KEY, category.name(), "outcome", outcome);
            }
        }
    }

    private static ExecutorService createExecutor() {
        return new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(64), runnable -> {
            Thread thread = new Thread(runnable, "slack-alert-delivery");
            thread.setDaemon(true);
            return thread;
        }, new ThreadPoolExecutor.AbortPolicy());
    }

    public void alert(SlackAlertCategory category, String errorCode, Throwable exception) {
        alert(category, errorCode, exception, "");
    }

    public void alert(SlackAlertCategory category, String errorCode, Throwable exception, String diagnostic) {
        Objects.requireNonNull(category);
        if (properties.webhookUrl().isBlank()) {
            count(category, "disabled");
            return;
        }
        String key = category + ":" + label(errorCode) + ":" + SensitiveDataRedactor.exceptionType(exception)
                + ":" + failureSite(exception);
        if (!reserveAlertSlot(key, nanoTime.getAsLong(), category)) {
            return;
        }
        // 호출자 문맥을 문자열로 고정하고 예외 객체와 MDC를 작업 스레드에 넘기지 않는다.
        String message = formatMessage(category, errorCode, exception, diagnostic);
        try {
            executor.execute(new Delivery(category, message,
                    label(MDC.get(CORRELATION_ID_KEY)), label(MDC.get(CRAWLER_RUN_ID_KEY))));
        } catch (RejectedExecutionException rejected) {
            releaseAlertSlot(key);
            count(category, REJECTED_OUTCOME);
            logDeliveryRejected(category);
        }
    }

    private synchronized boolean reserveAlertSlot(String key, long nowNanos, SlackAlertCategory category) {
        lastAlertNanos.entrySet().removeIf(entry -> nowNanos - entry.getValue() >= cooldownNanos);
        if (lastAlertNanos.containsKey(key)) {
            count(category, "suppressed");
            return false;
        }
        if (lastAlertNanos.size() >= MAX_DEDUPLICATION_KEYS) {
            count(category, REJECTED_OUTCOME);
            return false;
        }
        lastAlertNanos.put(key, nowNanos);
        return true;
    }

    private synchronized void releaseAlertSlot(String key) {
        lastAlertNanos.remove(key);
    }

    private String failureSite(Throwable exception) {
        if (exception == null || exception.getStackTrace().length == 0) {
            return "NONE";
        }
        for (StackTraceElement frame : exception.getStackTrace()) {
            if (frame.getClassName().startsWith("bhoon.sugang_helper.")) {
                return frame.toString();
            }
        }
        return exception.getStackTrace()[0].toString();
    }

    private void count(SlackAlertCategory category, String outcome) {
        registry.counter("slack.alert.delivery", CATEGORY_KEY, category.name(), "outcome", outcome).increment();
    }

    private void logDeliveryRejected(SlackAlertCategory category) {
        log.atWarn().addKeyValue("event", "slack_alert_delivery_rejected")
                .addKeyValue(CATEGORY_KEY, category).log("Slack 오류 알림 전송 작업 거부");
    }

    private void logDeliveryFailed(Delivery delivery, RuntimeException failure) {
        log.atWarn().addKeyValue("event", "slack_alert_delivery_failed")
                .addKeyValue(CATEGORY_KEY, delivery.category)
                .addKeyValue(CORRELATION_ID_KEY, delivery.correlationId)
                .addKeyValue(CRAWLER_RUN_ID_KEY, delivery.crawlerRunId)
                .addKeyValue("exceptionType", SensitiveDataRedactor.exceptionType(failure))
                .log("Slack 오류 알림 전송 실패");
    }

    @PreDestroy
    public void close() {
        executor.shutdown();
        try {
            if (executor.awaitTermination(5, TimeUnit.SECONDS)) {
                return;
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        int abandoned = 0;
        for (Runnable pending : executor.shutdownNow()) {
            if (pending instanceof Delivery delivery) {
                count(delivery.category, REJECTED_OUTCOME);
                abandoned++;
            }
        }
        if (abandoned > 0) {
            logAbandonedDeliveries(abandoned);
        }
    }

    private void logAbandonedDeliveries(int count) {
        log.atWarn().addKeyValue("event", "slack_alert_shutdown_abandoned")
                .addKeyValue("abandonedCount", count).log("Slack 오류 알림 종료 중 대기 작업 취소");
    }

    private final class Delivery implements Runnable {

        private final SlackAlertCategory category;
        private final String message;
        private final String correlationId;
        private final String crawlerRunId;

        private Delivery(SlackAlertCategory category, String message, String correlationId, String crawlerRunId) {
            this.category = category;
            this.message = message;
            this.correlationId = correlationId;
            this.crawlerRunId = crawlerRunId;
        }

        @Override
        public void run() {
            try {
                webhookClient.send(message);
                count(category, "success");
            } catch (RuntimeException failure) {
                count(category, "failure");
                logDeliveryFailed(this, failure);
            }
        }
    }

    private String formatMessage(SlackAlertCategory category, String errorCode, Throwable exception,
                                 String diagnostic) {
        String detail = safeDiagnostic(diagnostic);
        Objects.requireNonNull(category);
        String summary = summaryFor(category);
        String impact = impactFor(category);
        String retry = retryDescription(detail);
        String nextCheck = nextCheckFor(category);
        return String.join("\n",
                "[줍줍 오류][" + label(properties.environment()) + "] " + summary,
                "상태: " + statusFor(category) + " · 복구 여부 미확인",
                "영향: " + impact,
                "감지: " + ALERT_TIME_FORMAT.format(Instant.now()) + " (한국 시간)",
                "첫 대응: " + nextCheck,
                "오류: " + label(errorCode) + " · " + label(SensitiveDataRedactor.exceptionType(exception)),
                "진단 단서: " + diagnosticDescription(detail),
                "재시도: " + retry,
                "추적: 요청=" + label(MDC.get(CORRELATION_ID_KEY)) + " · 수집=" + label(MDC.get(CRAWLER_RUN_ID_KEY)),
                "Grafana 검색: " + logQuery());
    }

    private String statusFor(SlackAlertCategory category) {
        if (category == SlackAlertCategory.CRAWLER_FETCH) {
            return "이번 강의 수집 실패";
        }
        if (category == SlackAlertCategory.CRAWLER_PARSE) {
            return "이번 학교 응답 해석 실패";
        }
        if (category == SlackAlertCategory.DB_PERSIST) {
            return "이번 수집 결과 저장 실패";
        }
        if (category == SlackAlertCategory.SERVER_5XX) {
            return "해당 API 요청 실패";
        }
        throw new IllegalArgumentException(UNSUPPORTED_CATEGORY + category);
    }

    private String summaryFor(SlackAlertCategory category) {
        if (category == SlackAlertCategory.CRAWLER_FETCH) {
            return "강의 API 수집 실패";
        }
        if (category == SlackAlertCategory.CRAWLER_PARSE) {
            return "강의 데이터 해석 실패";
        }
        if (category == SlackAlertCategory.DB_PERSIST) {
            return "수집 결과 DB 저장 실패";
        }
        if (category == SlackAlertCategory.SERVER_5XX) {
            return "API 요청 처리 실패";
        }
        throw new IllegalArgumentException(UNSUPPORTED_CATEGORY + category);
    }

    private String impactFor(SlackAlertCategory category) {
        if (category == SlackAlertCategory.CRAWLER_FETCH || category == SlackAlertCategory.CRAWLER_PARSE) {
            return "최신 강의·여석 반영 지연 가능 · 기존 조회·영향 사용자 범위 미확인";
        }
        if (category == SlackAlertCategory.DB_PERSIST) {
            return "최신 강의·빈자리 알림 반영 지연 가능 · 저장 실패 범위·기존 데이터 상태 미확인";
        }
        if (category == SlackAlertCategory.SERVER_5XX) {
            return "해당 요청이 실패함 · 다른 요청·사용자 영향은 미확인";
        }
        throw new IllegalArgumentException(UNSUPPORTED_CATEGORY + category);
    }

    private String retryDescription(String detail) {
        if (detail.contains("retryable=true")) {
            return "재시도 가능한 유형 · 실제 예약·실행·성공 여부는 로그 확인";
        }
        if (detail.contains("retryable=false")) {
            return "자동 재시도 대상 아님 · 원인 확인 후 재실행 여부 판단";
        }
        return "실행 여부 미확인";
    }

    private String nextCheckFor(SlackAlertCategory category) {
        if (category == SlackAlertCategory.CRAWLER_FETCH) {
            return "① 같은 수집 ID의 실패·재시도 로그 ② 마지막 수집 성공 시각 ③ 다음 수집 결과 확인";
        }
        if (category == SlackAlertCategory.CRAWLER_PARSE) {
            return "① 해석 실패 위치 ② 학교 응답의 필수 항목·형식 변경 ③ 다음 수집 성공 여부 확인";
        }
        if (category == SlackAlertCategory.DB_PERSIST) {
            return "① DB 연결·제약 조건 오류 ② 같은 수집 ID의 저장 결과 ③ 강의·알림 반영 여부 확인";
        }
        if (category == SlackAlertCategory.SERVER_5XX) {
            return "① 요청 ID로 실패 API 확인 ② 같은 오류의 반복·다른 요청 실패 ③ 앱 상태 확인";
        }
        throw new IllegalArgumentException(UNSUPPORTED_CATEGORY + category);
    }

    private String logQuery() {
        String selector = "{app=\"jbnu-sugang-helper\",job=\"app\"} | json | __error__=\"\"";
        for (String key : List.of(CRAWLER_RUN_ID_KEY, CORRELATION_ID_KEY)) {
            String value = label(MDC.get(key));
            if (!value.equals("NONE") && !value.equals("INVALID")) {
                return selector + " | " + key + "=\"" + value + "\"";
            }
        }
        return "추적 ID 없음. 감지 시각 전후 5분의 app 오류 로그를 환경·오류 코드로 확인";
    }

    private String diagnosticDescription(String detail) {
        String description = detail.replace("failureStage=", "실패 단계=")
                .replace("failureType=", "원인 유형=")
                .replace("upstreamStatus=", "학교 응답=")
                .replace("retryable=true", "")
                .replace("retryable=false", "")
                .replace("retryable=UNKNOWN", "")
                .replace("UNKNOWN", "미확인")
                .replace("JUMP_BOOTSTRAP", "학교 API 접속 준비(JUMP_BOOTSTRAP)")
                .replace("COURSE_API", "학교 강의 API 호출(COURSE_API)")
                .replace("FETCH_PARSE", "강의 수집·해석(FETCH_PARSE)")
                .replace("PERSIST", "DB 저장(PERSIST)")
                .replace("SocketTimeoutException", "응답 대기 시간 초과(SocketTimeoutException)")
                .replace("ConnectException", "연결 실패(ConnectException)")
                .replace("UnknownHostException", "서버 주소 확인 실패(UnknownHostException)")
                .trim();
        if (description.isBlank()) {
            return "추가 메타데이터 없음";
        }
        return description;
    }

    private String safeDiagnostic(String diagnostic) {
        if (diagnostic == null || diagnostic.isBlank()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        appendDiagnostic(result, diagnostic, "failureStage", "[A-Z_]{1,40}");
        appendDiagnostic(result, diagnostic, "failureType", "[A-Za-z0-9_$]{1,100}");
        appendDiagnostic(result, diagnostic, "upstreamStatus", "[1-5][0-9]{2}|UNKNOWN");
        appendDiagnostic(result, diagnostic, "retryable", "true|false|UNKNOWN");
        return result.toString().trim();
    }

    private void appendDiagnostic(StringBuilder result, String diagnostic, String key, String valuePattern) {
        Matcher matcher = Pattern.compile("(?:^|\\s)" + key + "=(" + valuePattern + ")(?=\\s|$)")
                .matcher(diagnostic);
        if (matcher.find()) {
            result.append(key).append('=').append(matcher.group(1)).append(' ');
        }
    }

    private String label(String value) {
        if (value == null || value.isBlank()) {
            return "NONE";
        }
        if (!value.matches("[\\p{L}\\p{N}_.:-]{1,128}")) {
            return "INVALID";
        }
        return value;
    }

}
