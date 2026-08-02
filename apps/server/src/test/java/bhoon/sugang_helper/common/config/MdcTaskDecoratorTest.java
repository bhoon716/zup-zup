package bhoon.sugang_helper.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
    void copiesSubmittingContextAndClearsItBeforeWorkerReuse() throws Exception {
        MdcTaskDecorator decorator = new MdcTaskDecorator();
        AtomicReference<String> observedCorrelationId = new AtomicReference<>();

        MDC.put(CORRELATION_ID_KEY, "request-123");
        executor.submit(decorator.decorate(() -> {
            observedCorrelationId.set(MDC.get(CORRELATION_ID_KEY));
            MDC.put(WORKER_ONLY_KEY, "must-be-cleared");
        })).get();
        MDC.clear();

        String leakedCorrelationId = executor.submit(() -> MDC.get(CORRELATION_ID_KEY)).get();
        String leakedWorkerValue = executor.submit(() -> MDC.get(WORKER_ONLY_KEY)).get();

        assertThat(observedCorrelationId).hasValue("request-123");
        assertThat(leakedCorrelationId).isNull();
        assertThat(leakedWorkerValue).isNull();
    }
}
