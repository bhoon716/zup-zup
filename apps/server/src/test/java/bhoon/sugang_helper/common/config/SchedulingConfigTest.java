package bhoon.sugang_helper.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
    @ValueSource(booleans = {false, true})
    void dueTasksDoNotOverlapWhileALongTaskRunsEvenWhenVirtualThreadsAreEnabled(boolean virtualThreads) {
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
    void closingTheContextCancelsPendingTasksAndTerminatesTheScheduler() throws InterruptedException {

        AtomicReference<ScheduledThreadPoolExecutor> executor = new AtomicReference<>();
        AtomicReference<ScheduledFuture<?>> pending = new AtomicReference<>();
        AtomicInteger executions = new AtomicInteger();


        contextRunner.withPropertyValues("spring.threads.virtual.enabled=true").run(context -> {
            ThreadPoolTaskScheduler scheduler = context.getBean("taskScheduler", ThreadPoolTaskScheduler.class);
            executor.set(scheduler.getScheduledThreadPoolExecutor());
            pending.set(scheduler.schedule(executions::incrementAndGet, Instant.now().plusSeconds(3_600)));
        });


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
