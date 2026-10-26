package bhoon.sugang_helper.common.alert;

import bhoon.sugang_helper.common.security.util.SensitiveDataRedactor;
import java.time.Instant;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SlackAlertService {

    private static final int MAX_DEDUPLICATION_KEYS = 256;
    private static final int MAX_STACK_TRACE_FRAMES = 3;
    private final SlackAlertProperties properties;
    private final SlackWebhookClient webhookClient;
    private final long cooldownNanos;
    private final Map<String, Long> lastAlertNanos = new LinkedHashMap<>();

    public SlackAlertService(SlackAlertProperties properties, SlackWebhookClient webhookClient) {
        this.properties = properties;
        this.webhookClient = webhookClient;
        this.cooldownNanos = TimeUnit.SECONDS.toNanos(properties.cooldownSeconds());
    }

    public void alert(SlackAlertCategory category, String errorCode, Throwable exception) {
        alert(category, errorCode, exception, "");
    }

    public void alert(SlackAlertCategory category, String errorCode, Throwable exception, String diagnostic) {
        if (properties.webhookUrl().isBlank()) {
            log.debug("Slack webhook 미설정으로 오류 알림 생략. category={}", category);
            return;
        }

        String deduplicationKey = category + ":" + errorCode + ":" + SensitiveDataRedactor.exceptionType(exception);
        if (!reserveAlertSlot(deduplicationKey, System.nanoTime())) {
            log.debug("중복 제한 시간 내 Slack 오류 알림 생략. category={}, errorCode={}", category, errorCode);
            return;
        }

        String message = formatMessage(category, errorCode, exception, diagnostic);
        try {
            webhookClient.send(message);
        } catch (RuntimeException sendFailure) {
            log.warn("Slack 오류 알림 전송 실패. category={}, errorCode={}, exceptionType={}",
                    category, errorCode, SensitiveDataRedactor.exceptionType(sendFailure));
        }
    }

    private synchronized boolean reserveAlertSlot(String key, long nowNanos) {
        Long previousNanos = lastAlertNanos.get(key);
        if (previousNanos != null && nowNanos - previousNanos < cooldownNanos) {
            return false;
        }
        if (previousNanos == null && lastAlertNanos.size() >= MAX_DEDUPLICATION_KEYS) {
            Iterator<String> iterator = lastAlertNanos.keySet().iterator();
            iterator.next();
            iterator.remove();
        }
        lastAlertNanos.put(key, nowNanos);
        return true;
    }

    private String formatMessage(SlackAlertCategory category, String errorCode, Throwable exception,
                                 String diagnostic) {
        String detail = safeDiagnostic(diagnostic);
        String summary = switch (category) {
            case CRAWLER_FETCH -> "강의 API 수집 실패";
            case CRAWLER_PARSE -> "강의 데이터 해석 실패";
            case DB_PERSIST -> "수집 결과 DB 저장 실패";
            case SERVER_5XX -> "API 요청 처리 실패";
        };
        String impact = switch (category) {
            case CRAWLER_FETCH, CRAWLER_PARSE -> "이번 수집 실패. 기존 조회와 데이터 신선도 영향은 확인 필요";
            case DB_PERSIST -> "이번 수집 저장 실패. 최신 강의·빈자리 알림 반영 여부 확인 필요";
            case SERVER_5XX -> "해당 요청이 실패함. 다른 요청·사용자 영향은 확인 필요";
        };
        String retry = "확인 필요. 이 알림 자체는 작업을 재실행하지 않음";
        if (detail.contains("retryable=true")) {
            retry = "재시도 가능한 유형. 이번 실행의 횟수 소진·다음 예약 여부는 로그 확인 필요";
        } else if (detail.contains("retryable=false")) {
            retry = "자동 재시도 대상이 아닌 유형. 다음 실행·수동 조치 여부 확인 필요";
        }
        return String.join("\n",
                "[줍줍 오류] " + summary,
                "환경: " + label(properties.environment()) + " | 발생 시각(UTC): " + Instant.now(),
                "영향: " + impact,
                "긴급도: 확인 필요. 반복 여부·신선도·실패 요청 범위를 확인한 뒤 판단",
                "재시도: " + retry,
                "식별: category=" + category + " errorCode=" + label(errorCode)
                        + " exceptionType=" + label(SensitiveDataRedactor.exceptionType(exception)),
                "추적: correlationId=" + label(MDC.get("correlationId"))
                        + " crawlerRunId=" + label(MDC.get("crawlerRunId")),
                "진단: " + (detail.isBlank() ? "추가 메타데이터 없음" : detail),
                "상위 호출 위치: stackTraceTop3=" + stackTraceTop3(exception),
                "다음 확인: Grafana에서 환경과 위 추적 ID로 검색 → 오류 코드·원인 타입·stack trace 확인");
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
        return value.matches("[\\p{L}\\p{N}_.:-]{1,128}") ? value : "INVALID";
    }

    private String stackTraceTop3(Throwable exception) {
        if (exception == null || exception.getStackTrace().length == 0) {
            return "<none>";
        }
        return Arrays.stream(exception.getStackTrace())
                .limit(MAX_STACK_TRACE_FRAMES)
                .map(StackTraceElement::toString)
                .reduce((left, right) -> left + " | " + right)
                .orElse("<none>");
    }
}
