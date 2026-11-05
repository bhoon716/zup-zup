package bhoon.sugang_helper.seatalert.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.academic.events.SeatOpenedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionalEventListener;

class SeatOpenedListenerTest {

    @Test
    void synchronouslyDelegatesToTheExistingOutbox() throws Exception {
        SeatNotificationOutboxService service = mock(SeatNotificationOutboxService.class);
        SeatOpenedListener listener = new SeatOpenedListener(service);
        SeatOpenedEvent event = new SeatOpenedEvent("course", "강의", "교수", 0, 1);
        var method = SeatOpenedListener.class.getMethod("on", SeatOpenedEvent.class);

        listener.on(event);

        verify(service).enqueue(event);
        assertThat(AnnotatedElementUtils.hasAnnotation(method, EventListener.class)).isTrue();
        assertThat(AnnotatedElementUtils.hasAnnotation(method, Async.class)).isFalse();
        assertThat(AnnotatedElementUtils.hasAnnotation(method, TransactionalEventListener.class)).isFalse();
    }

    @Test
    void outboxFailureIsNotSwallowed() {
        SeatNotificationOutboxService service = mock(SeatNotificationOutboxService.class);
        SeatOpenedListener listener = new SeatOpenedListener(service);
        SeatOpenedEvent event = new SeatOpenedEvent("course", "강의", "교수", 0, 1);
        IllegalStateException failure = new IllegalStateException("forced outbox failure");
        doThrow(failure).when(service).enqueue(event);

        assertThatThrownBy(() -> listener.on(event)).isSameAs(failure);
    }
}
