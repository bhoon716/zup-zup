package bhoon.sugang_helper.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

class SchedulingConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TaskSchedulingAutoConfiguration.class))
            .withUserConfiguration(SchedulingConfig.class, SchedulingEnabled.class)
            .withPropertyValues("spring.task.scheduling.pool.size=8");

    @ParameterizedTest
    @DisplayName("가상 스레드가 활성화돼도 오래 걸리는 예약 작업은 겹치지 않는다")
    @ValueSource(booleans = {false, true})
    void dueTasksDoNotOverlapWhileALongTaskRunsEvenWhenVirtualThreadsAreEnabled(boolean virtualThreads) {
        // when & then
        contextRunner.withPropertyValues("spring.threads.virtual.enabled=" + virtualThreads).run(context -> {

            assertThat(context).hasSingleBean(TaskScheduler.class);
            ThreadPoolTaskScheduler scheduler = context.getBean("taskScheduler", ThreadPoolTaskScheduler.class);
            assertThat(scheduler.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(1);
            CountDownLatch firstStarted = new CountDownLatch(1);
            CountDownLatch releaseFirst = new CountDownLatch(1);
            CountDownLatch secondStarted = new CountDownLatch(1);
            scheduler.schedule(() -> waitForRelease(firstStarted, releaseFirst), Instant.now());

            try {

                assertThat(firstStarted.await(2, TimeUnit.SECONDS)).isTrue();
                scheduler.schedule(secondStarted::countDown, Instant.now());

                assertThat(secondStarted.await(100, TimeUnit.MILLISECONDS)).isFalse();
                releaseFirst.countDown();
                assertThat(secondStarted.await(2, TimeUnit.SECONDS)).isTrue();
            } finally {
                releaseFirst.countDown();
            }
        });
    }

    @Test
    @DisplayName("컨텍스트 종료는 대기 작업을 취소하고 스케줄러를 종료한다")
    void closingTheContextCancelsPendingTasksAndTerminatesTheScheduler() throws InterruptedException {
        // given
        AtomicReference<ScheduledThreadPoolExecutor> executor = new AtomicReference<>();
        AtomicReference<ScheduledFuture<?>> pending = new AtomicReference<>();
        AtomicInteger executions = new AtomicInteger();

        // when
        contextRunner.withPropertyValues("spring.threads.virtual.enabled=true").run(context -> {
            ThreadPoolTaskScheduler scheduler = context.getBean("taskScheduler", ThreadPoolTaskScheduler.class);
            executor.set(scheduler.getScheduledThreadPoolExecutor());
            pending.set(scheduler.schedule(executions::incrementAndGet, Instant.now().plusSeconds(3_600)));
        });

        // then
        assertThat(executor.get().isShutdown()).isTrue();
        assertThat(executor.get().awaitTermination(2, TimeUnit.SECONDS)).isTrue();
        assertThat(pending.get().isCancelled()).isTrue();
        assertThat(executions).hasValue(0);
    }

    private void waitForRelease(CountDownLatch started, CountDownLatch release) {
        started.countDown();
        try {
            release.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    static class SchedulingEnabled {
    }
}
