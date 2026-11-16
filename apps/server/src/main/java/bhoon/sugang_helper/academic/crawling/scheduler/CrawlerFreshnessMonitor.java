package bhoon.sugang_helper.academic.crawling.scheduler;

import bhoon.sugang_helper.academic.crawling.service.CrawlerRunStateService;
import bhoon.sugang_helper.common.api.security.SensitiveDataRedactor;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CrawlerFreshnessMonitor {

    private static final String EVENT_KEY = "event";
    private static final String ADMIN_ALERT_TARGET = "ADMIN";

    private final CrawlerRunStateService crawlerRunStateService;
    private final MeterRegistry meterRegistry;
    private final AtomicLong freshnessAgeSeconds = new AtomicLong(-1);
    private final AtomicLong staleGauge = new AtomicLong(1);
    private final AtomicLong lastSuccessTimestamp = new AtomicLong();
    private final AtomicLong freshnessCheckTimestamp = new AtomicLong();
    private final AtomicLong freshnessCheckHealthy = new AtomicLong();
    private final AtomicBoolean staleAlertActive = new AtomicBoolean(false);

    @Value("${jbnu.crawler.stale-threshold-minutes:15}")
    private long staleThresholdMinutes;

    public CrawlerFreshnessMonitor(CrawlerRunStateService crawlerRunStateService, MeterRegistry meterRegistry) {
        this.crawlerRunStateService = crawlerRunStateService;
        this.meterRegistry = meterRegistry;
        if (meterRegistry != null) {
            meterRegistry.gauge("crawler.data.freshness.age.seconds", freshnessAgeSeconds);
            meterRegistry.gauge("crawler.data.stale", staleGauge);
            meterRegistry.gauge("crawler.data.last.success.timestamp.seconds", lastSuccessTimestamp);
            meterRegistry.gauge("crawler.data.freshness.check.last.success.timestamp.seconds", freshnessCheckTimestamp);
            meterRegistry.gauge("crawler.data.freshness.check.healthy", freshnessCheckHealthy);
            meterRegistry.gauge("crawler.data.freshness.threshold.seconds", this,
                    monitor -> Duration.ofMinutes(monitor.staleThresholdMinutes).toSeconds());
        }
    }

    @Scheduled(fixedDelayString = "${jbnu.crawler.freshness-check-ms:60000}", scheduler = "monitoringTaskScheduler")
    public void checkDataFreshness() {
        if (crawlerRunStateService == null || meterRegistry == null) {
            return;
        }
        try {
            refreshDataFreshness();
            freshnessCheckHealthy.set(1);
            freshnessCheckTimestamp.set(Instant.now().getEpochSecond());
        } catch (RuntimeException exception) {
            freshnessCheckHealthy.set(0);
            freshnessAgeSeconds.set(-1);
            staleGauge.set(1);
            logFreshnessCheckFailed(exception);
        }
    }

    private void refreshDataFreshness() {
        Optional<LocalDateTime> latest = crawlerRunStateService.findLastSuccessAt();
        lastSuccessTimestamp.set(latest.map(value -> value.atZone(ZoneId.systemDefault()).toEpochSecond()).orElse(0L));
        long ageSeconds = latest.map(value -> Math.max(0, Duration.between(value, LocalDateTime.now()).toSeconds()))
                .orElse(-1L);
        freshnessAgeSeconds.set(ageSeconds);
        boolean stale = ageSeconds < 0 || ageSeconds > Duration.ofMinutes(staleThresholdMinutes).toSeconds();
        if (!stale) {
            staleGauge.set(0);
            staleAlertActive.set(false);
            return;
        }
        staleGauge.set(1);
        if (!staleAlertActive.compareAndSet(false, true)) {
            return;
        }
        logDataStale(ageSeconds);
    }

    private void logFreshnessCheckFailed(RuntimeException exception) {
        log.atError().addKeyValue(EVENT_KEY, "crawler_freshness_check_failed")
                .addKeyValue("exceptionType", SensitiveDataRedactor.exceptionType(exception))
                .log("[Crawler] 강의 데이터 신선도 확인 실패.");
    }

    private void logDataStale(long ageSeconds) {
        log.atError().addKeyValue(EVENT_KEY, "crawler_data_stale").addKeyValue("ageSeconds", ageSeconds)
                .addKeyValue("thresholdMinutes", staleThresholdMinutes).addKeyValue("alert", ADMIN_ALERT_TARGET)
                .log("[Crawler] 강의 데이터 신선도 기준 초과. ageSeconds={}, thresholdMinutes={}, alert=" + ADMIN_ALERT_TARGET,
                        ageSeconds, staleThresholdMinutes);
    }
}
