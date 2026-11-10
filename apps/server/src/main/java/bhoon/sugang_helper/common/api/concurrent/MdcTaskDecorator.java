package bhoon.sugang_helper.common.api.concurrent;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

@Component
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> runWithContext(runnable, contextMap);
    }

    private void runWithContext(Runnable runnable, Map<String, String> contextMap) {
        try {
            applyContext(contextMap);
            runnable.run();
        } finally {
            MDC.clear();
        }
    }

    private void applyContext(Map<String, String> contextMap) {
        if (contextMap == null || contextMap.isEmpty()) {
            MDC.clear();
            return;
        }
        MDC.setContextMap(contextMap);
    }
}
