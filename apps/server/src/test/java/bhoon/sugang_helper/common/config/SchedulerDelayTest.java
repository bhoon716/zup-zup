package bhoon.sugang_helper.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.academic.course.dto.external.ParsedCourseDto;
import bhoon.sugang_helper.academic.course.enums.SemesterType;
import bhoon.sugang_helper.academic.crawling.dto.result.CrawlTargetInfo;
import bhoon.sugang_helper.academic.crawling.scheduler.CourseScheduler;
import bhoon.sugang_helper.academic.crawling.service.CourseCrawlCompletionService;
import bhoon.sugang_helper.academic.crawling.service.CourseCrawlFetcher;
import bhoon.sugang_helper.academic.crawling.service.CourseCrawlerService;
import bhoon.sugang_helper.academic.crawling.service.CourseCrawlerTargetService;
import bhoon.sugang_helper.academic.crawling.service.CrawlerRunStateService;
import bhoon.sugang_helper.seatalert.notification.scheduler.SeatNotificationOutboxWorker;
import bhoon.sugang_helper.seatalert.notification.service.SeatNotificationOutboxProcessor;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.boot.task.ThreadPoolTaskSchedulerBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

class SchedulerDelayTest {

    private static final String YEAR = "2026";
    private static final String SEMESTER = "U211600010";
    private final AtomicInteger activeTasks = new AtomicInteger();
    private final AtomicInteger maximumActiveTasks = new AtomicInteger();

    @Test
    void slowCrawlDelaysOutboxAndFreshnessChecksWithoutOverlapAndBothRecover() throws Exception {

        CourseCrawlFetcher fetcher = mock(CourseCrawlFetcher.class);
        CourseCrawlCompletionService completion = mock(CourseCrawlCompletionService.class);
        CourseCrawlerTargetService targetService = mock(CourseCrawlerTargetService.class);
        CrawlerRunStateService runState = mock(CrawlerRunStateService.class);
        SeatNotificationOutboxProcessor processor = mock(SeatNotificationOutboxProcessor.class);
        CountDownLatch fetchStarted = new CountDownLatch(1);
        CountDownLatch releaseFetch = new CountDownLatch(1);
        CountDownLatch dependentStarted = new CountDownLatch(1);
        CountDownLatch outboxCompletedTwice = new CountDownLatch(2);
        CountDownLatch freshnessCompletedTwice = new CountDownLatch(2);
        AtomicLong fetchStartedAt = new AtomicLong();
        AtomicLong outboxStartedAt = new AtomicLong();
        AtomicLong freshnessStartedAt = new AtomicLong();
        List<ParsedCourseDto> courses = List.of(mock(ParsedCourseDto.class));
        when(targetService.getCurrentTargetValue()).thenReturn(new CrawlTargetInfo(YEAR, SemesterType.FIRST_SEMESTER));
        when(fetcher.fetch(YEAR, SEMESTER)).thenAnswer(invocation -> {
            fetchStartedAt.set(System.nanoTime());
            fetchStarted.countDown();
            if (!releaseFetch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting to release the fake school response");
            }
            return courses;
        });
        when(processor.claimReadyDeliveries(1)).thenReturn(List.of());
        when(runState.findLastSuccessAt()).thenReturn(Optional.of(LocalDateTime.now()));

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        try {
            CourseCrawlerService crawler = new CourseCrawlerService(fetcher, completion, targetService, runState, registry);
            ReflectionTestUtils.setField(crawler, "staleThresholdMinutes", 15L);
            CourseScheduler courseScheduler = new CourseScheduler(crawler);
            SeatNotificationOutboxWorker worker = new SeatNotificationOutboxWorker(processor, registry, 1);
            ThreadPoolTaskScheduler scheduler = new SchedulingConfig()
                    .taskScheduler(new ThreadPoolTaskSchedulerBuilder());
            scheduler.initialize();
            try {
                ScheduledFuture<?> crawl = scheduler.schedule(observe(courseScheduler::runCrawler), Instant.now());
                assertThat(fetchStarted.await(2, TimeUnit.SECONDS)).isTrue();
                long enqueuedAt = System.nanoTime();
                Instant due = Instant.now();

                ScheduledFuture<?> outbox = scheduler.scheduleWithFixedDelay(observe(() -> {
                    outboxStartedAt.compareAndSet(0, System.nanoTime());
                    dependentStarted.countDown();
                    worker.processPendingNotifications();
                    outboxCompletedTwice.countDown();
                }), due, Duration.ofMillis(5));
                ScheduledFuture<?> freshness = scheduler.scheduleWithFixedDelay(observe(() -> {
                    freshnessStartedAt.compareAndSet(0, System.nanoTime());
                    dependentStarted.countDown();
                    crawler.checkDataFreshness();
                    freshnessCompletedTwice.countDown();
                }), due, Duration.ofMillis(60));


                assertThat(dependentStarted.await(300, TimeUnit.MILLISECONDS)).isFalse();
                assertThat(outboxStartedAt.get()).isZero();
                assertThat(freshnessStartedAt.get()).isZero();
                long releasedAt = System.nanoTime();
                releaseFetch.countDown();
                crawl.get(2, TimeUnit.SECONDS);


                assertThat(outboxCompletedTwice.await(2, TimeUnit.SECONDS)).isTrue();
                assertThat(freshnessCompletedTwice.await(2, TimeUnit.SECONDS)).isTrue();
                outbox.cancel(false);
                freshness.cancel(false);
                assertThat(outboxStartedAt.get()).isGreaterThanOrEqualTo(releasedAt);
                assertThat(freshnessStartedAt.get()).isGreaterThanOrEqualTo(releasedAt);
                assertThat(maximumActiveTasks.get()).isEqualTo(1);
                assertThat(registry.get("crawler.runs").tag("status", "SUCCEEDED").counter().count()).isEqualTo(1);
                assertThat(registry.get("crawler.data.stale").gauge().value()).isZero();
                System.out.printf("SCHEDULER_DELAY_PROBE holdMs=%d outboxDelayMs=%d freshnessDelayMs=%d "
                                + "maximumActiveJobs=%d outboxConfirmedPolls=%d freshnessConfirmedPolls=%d%n",
                        elapsedMillis(fetchStartedAt.get(), releasedAt),
                        elapsedMillis(enqueuedAt, outboxStartedAt.get()),
                        elapsedMillis(enqueuedAt, freshnessStartedAt.get()),
                        maximumActiveTasks.get(), 2 - outboxCompletedTwice.getCount(),
                        2 - freshnessCompletedTwice.getCount());
            } finally {
                releaseFetch.countDown();
                worker.shutdown();
                scheduler.shutdown();
                assertThat(scheduler.getScheduledThreadPoolExecutor().awaitTermination(2, TimeUnit.SECONDS)).isTrue();
            }
        } finally {
            registry.close();
        }
    }

    private Runnable observe(Runnable task) {
        return () -> {
            maximumActiveTasks.accumulateAndGet(activeTasks.incrementAndGet(), Math::max);
            try {
                task.run();
            } finally {
                activeTasks.decrementAndGet();
            }
        };
    }

    private long elapsedMillis(long startedAt, long finishedAt) {
        return TimeUnit.NANOSECONDS.toMillis(finishedAt - startedAt);
    }
}
