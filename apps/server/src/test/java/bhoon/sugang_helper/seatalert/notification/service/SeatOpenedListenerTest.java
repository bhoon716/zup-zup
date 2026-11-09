package bhoon.sugang_helper.seatalert.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.academic.events.SeatOpenedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionalEventListener;

class SeatOpenedListenerTest {

    @Test
    @DisplayName("좌석 리스너는 기존 Outbox 서비스에 동기 위임한다")
    void synchronouslyDelegatesToTheExistingOutbox() throws Exception {
        // given
        SeatNotificationOutboxService service = mock(SeatNotificationOutboxService.class);
        SeatOpenedListener listener = new SeatOpenedListener(service);
        SeatOpenedEvent event = new SeatOpenedEvent("course", "강의", "교수", 0, 1);
        var method = SeatOpenedListener.class.getMethod("on", SeatOpenedEvent.class);

        // when
        listener.on(event);

        // then
        verify(service).enqueue(event);
        assertThat(AnnotatedElementUtils.hasAnnotation(method, EventListener.class)).isTrue();
        assertThat(AnnotatedElementUtils.hasAnnotation(method, Async.class)).isFalse();
        assertThat(AnnotatedElementUtils.hasAnnotation(method, TransactionalEventListener.class)).isFalse();
    }

    @Test
    @DisplayName("좌석 리스너는 Outbox 실패를 삼키지 않는다")
    void outboxFailureIsNotSwallowed() {
        // given
        SeatNotificationOutboxService service = mock(SeatNotificationOutboxService.class);
        SeatOpenedListener listener = new SeatOpenedListener(service);
        SeatOpenedEvent event = new SeatOpenedEvent("course", "강의", "교수", 0, 1);
        IllegalStateException failure = new IllegalStateException("forced outbox failure");
        doThrow(failure).when(service).enqueue(event);

        // when & then
        assertThatThrownBy(() -> listener.on(event)).isSameAs(failure);
    }
}
