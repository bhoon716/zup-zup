package bhoon.sugang_helper.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.boot.actuate.autoconfigure.metrics.PropertiesMeterFilter;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

class LatencyMetricsConfigurationTest {

    private static final List<String> METERS = List.of(
            "http.server.requests", "notification.provider.latency", "crawler.upstream.latency",
            "hikaricp.connections.acquire");

    @Test
    void productionSettingsExportAggregatableBucketsForEveryLatencyPanel() throws IOException {
        PrometheusMeterRegistry registry = registry(new MockClock());
        try {
            for (String name : METERS) {
                Timer timer = registry.timer(name);
                timer.record(Duration.ofNanos(10_000));
                timer.record(Duration.ofMillis(20));
                timer.record(Duration.ofMillis(90));

                String family = name.replace('.', '_') + "_seconds";
                String scrape = registry.scrape();
                assertThat(scrape).contains(family + "_bucket", family + "_sum", family + "_count", family + "_max");
                assertThat(scrape).doesNotContain(family + "_min");
                assertThat(timer.count()).isEqualTo(3);
                assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isCloseTo(110.01,
                        offset(0.0001));
            }

            Timer slowUpstream = registry.get("crawler.upstream.latency").timer();
            assertThat(slowUpstream.takeSnapshot().histogramCounts())
                    .anySatisfy(bucket -> assertThat(bucket.bucket(TimeUnit.SECONDS)).isBetween(240.0, 600.0));
            assertThat(registry.timer("unrelated.duration").takeSnapshot().histogramCounts()).isEmpty();
        } finally {
            registry.close();
        }
    }

    @Test
    void exportedMaxExpiresWhileCumulativeRequestCountRemains() throws IOException {
        MockClock clock = new MockClock();
        PrometheusMeterRegistry registry = registry(clock);
        try {
            Timer timer = registry.timer("http.server.requests");
            timer.record(Duration.ofMillis(25));
            assertThat(timer.max(TimeUnit.MILLISECONDS)).isEqualTo(25.0);

            clock.add(Duration.ofMinutes(4));

            assertThat(timer.max(TimeUnit.MILLISECONDS)).isZero();
            assertThat(timer.count()).isEqualTo(1);
        } finally {
            registry.close();
        }
    }

    private PrometheusMeterRegistry registry(MockClock clock) throws IOException {
        var source = new YamlPropertySourceLoader()
                .load("production-metrics", new ClassPathResource("application-prod.yml")).getFirst();
        MetricsProperties properties = new Binder(ConfigurationPropertySources.from(source))
                .bind("management.metrics", Bindable.of(MetricsProperties.class)).get();
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(
                PrometheusConfig.DEFAULT, new PrometheusRegistry(), clock);
        registry.config().meterFilter(new PropertiesMeterFilter(properties));
        return registry;
    }
}
