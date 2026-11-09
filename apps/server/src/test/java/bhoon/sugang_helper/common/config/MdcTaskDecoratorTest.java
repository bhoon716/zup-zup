package bhoon.sugang_helper.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.common.api.concurrent.MdcTaskDecorator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class MdcTaskDecoratorTest {

    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final String WORKER_ONLY_KEY = "workerOnly";

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        executor.shutdownNow();
    }

    @Test
    @DisplayName("제출 스레드의 문맥을 복사하고 작업자 재사용 전에 제거한다")
    void copiesSubmittingContextAndClearsItBeforeWorkerReuse() throws Exception {
        // given
        MdcTaskDecorator decorator = new MdcTaskDecorator();
        AtomicReference<String> observedCorrelationId = new AtomicReference<>();

        MDC.put(CORRELATION_ID_KEY, "request-123");

        // when
        executor.submit(decorator.decorate(() -> {
            observedCorrelationId.set(MDC.get(CORRELATION_ID_KEY));
            MDC.put(WORKER_ONLY_KEY, "must-be-cleared");
        })).get();
        MDC.clear();

        // then
        String leakedCorrelationId = executor.submit(() -> MDC.get(CORRELATION_ID_KEY)).get();
        String leakedWorkerValue = executor.submit(() -> MDC.get(WORKER_ONLY_KEY)).get();

        assertThat(observedCorrelationId).hasValue("request-123");
        assertThat(leakedCorrelationId).isNull();
        assertThat(leakedWorkerValue).isNull();
    }
}
