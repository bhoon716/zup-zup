package bhoon.sugang_helper.academic.crawling.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import bhoon.sugang_helper.academic.crawling.service.CrawlerRunStateService;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CrawlerFreshnessMonitorTest {

    private static final String STALE_THRESHOLD_FIELD = "staleThresholdMinutes";
    private static final String FRESHNESS_AGE_METRIC = "crawler.data.freshness.age.seconds";
    private static final String STALE_METRIC = "crawler.data.stale";
    private static final String LAST_SUCCESSFUL_CHECK_METRIC = "crawler.data.freshness.check.last.success.timestamp.seconds";
    private static final String CHECK_HEALTH_METRIC = "crawler.data.freshness.check.healthy";

    @Mock
    private CrawlerRunStateService crawlerRunStateService;

    @Test
    @DisplayName("crawler_status의 마지막 성공 시각이 임계치를 넘으면 관리자 경고를 남긴다")
    void staleDataEmitsAdminAlert() {
        // given
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CrawlerFreshnessMonitor monitor = new CrawlerFreshnessMonitor(crawlerRunStateService, registry);
        ReflectionTestUtils.setField(monitor, STALE_THRESHOLD_FIELD, 15L);
        given(crawlerRunStateService.findLastSuccessAt())
                .willReturn(Optional.of(LocalDateTime.now().minusMinutes(20)));
        Logger logger = (Logger) LoggerFactory.getLogger(CrawlerFreshnessMonitor.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        logger.addAppender(appender);
        appender.start();

        // when & then
        try {
            monitor.checkDataFreshness();

            assertThat(appender.list)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .anyMatch(message -> message.contains("강의 데이터 신선도 기준 초과")
                            && message.contains("alert=ADMIN"));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    @DisplayName("freshness gauge는 모니터 생성 시 한 번 등록된다")
    void freshnessGaugesAreRegisteredAtConstruction() {
        // given
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        new CrawlerFreshnessMonitor(crawlerRunStateService, registry);

        // when & then
        assertThat(registry.find(FRESHNESS_AGE_METRIC).gauges()).hasSize(1);
        assertThat(registry.find(STALE_METRIC).gauges()).hasSize(1);
    }

    @Test
    @DisplayName("신선도 조회 실패는 이전 정상값 대신 알 수 없는 상태와 실패 지표를 남긴다")
    void freshnessReadFailureInvalidatesPreviousHealthyState() {
        // given
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CrawlerFreshnessMonitor monitor = new CrawlerFreshnessMonitor(crawlerRunStateService, registry);
        ReflectionTestUtils.setField(monitor, STALE_THRESHOLD_FIELD, 15L);
        given(crawlerRunStateService.findLastSuccessAt())
                .willReturn(Optional.of(LocalDateTime.now().minusMinutes(1)))
                .willThrow(new IllegalStateException("password=private-database-secret"));
        monitor.checkDataFreshness();
        double checkedAt = registry.get(LAST_SUCCESSFUL_CHECK_METRIC).gauge().value();

        // when
        monitor.checkDataFreshness();

        // then
        assertThat(registry.get(CHECK_HEALTH_METRIC).gauge().value()).isZero();
        assertThat(registry.get(STALE_METRIC).gauge().value()).isEqualTo(1);
        assertThat(registry.get(FRESHNESS_AGE_METRIC).gauge().value()).isEqualTo(-1);
        assertThat(registry.get(LAST_SUCCESSFUL_CHECK_METRIC).gauge().value())
                .isEqualTo(checkedAt);
    }

    @Test
    @DisplayName("마지막 수집 성공과 검사 성공 시각을 별도로 노출해 멈춘 검사도 감지한다")
    void exposesDataAndCheckTimestampsSeparately() {
        // given
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CrawlerFreshnessMonitor monitor = new CrawlerFreshnessMonitor(crawlerRunStateService, registry);
        ReflectionTestUtils.setField(monitor, STALE_THRESHOLD_FIELD, 15L);
        LocalDateTime successAt = LocalDateTime.now().minusMinutes(20);
        given(crawlerRunStateService.findLastSuccessAt()).willReturn(Optional.of(successAt));

        // when
        monitor.checkDataFreshness();

        // then
        double successTimestamp = registry.get("crawler.data.last.success.timestamp.seconds").gauge().value();
        assertThat(successTimestamp).isEqualTo(successAt.atZone(ZoneId.systemDefault()).toEpochSecond());
        assertThat(registry.get(LAST_SUCCESSFUL_CHECK_METRIC).gauge().value())
                .isGreaterThan(successTimestamp);
        assertThat(registry.get(CHECK_HEALTH_METRIC).gauge().value()).isEqualTo(1);
        assertThat(registry.get("crawler.data.freshness.threshold.seconds").gauge().value()).isEqualTo(900);
    }
}
