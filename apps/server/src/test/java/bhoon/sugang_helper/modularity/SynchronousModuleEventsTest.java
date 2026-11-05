package bhoon.sugang_helper.modularity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import bhoon.sugang_helper.academic.events.SeatOpenedEvent;
import bhoon.sugang_helper.identity.events.UserWithdrawnEvent;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationOutboxRepository;
import bhoon.sugang_helper.seatalert.notification.service.SeatNotificationOutboxService;
import bhoon.sugang_helper.seatalert.subscription.service.SubscriptionAccountLifecycleService;
import bhoon.sugang_helper.support.feedback.service.FeedbackAccountLifecycleService;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

class SynchronousModuleEventsTest {

    private final SeatOpenedEvent event = new SeatOpenedEvent("course", "강의", "교수", 0, 1);
    private DataSource dataSource;
    private Connection connection;
    private SeatNotificationOutboxRepository repository;
    private DataSourceTransactionManager transactions;
    private SimpleApplicationEventMulticaster events;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        repository = mock(SeatNotificationOutboxRepository.class);
        given(dataSource.getConnection()).willReturn(connection);
        given(connection.getAutoCommit()).willReturn(true);
        transactions = new DataSourceTransactionManager(dataSource);

        ProxyFactory factory = new ProxyFactory(new SeatNotificationOutboxService(repository));
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(transactions);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        factory.addAdvice(interceptor);
        SeatNotificationOutboxService listener = (SeatNotificationOutboxService) factory.getProxy();
        events = new SimpleApplicationEventMulticaster();
        events.addApplicationListener(new ApplicationListener<PayloadApplicationEvent<SeatOpenedEvent>>() {
            @Override
            public void onApplicationEvent(PayloadApplicationEvent<SeatOpenedEvent> published) {
                listener.enqueue(published.getPayload());
            }
        });
    }

    @AfterEach
    void transactionContextDoesNotLeak() {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        assertThat(TransactionSynchronizationManager.hasResource(dataSource)).isFalse();
    }

    @Test
    void outboxListenerJoinsTheCallingTransactionAndThread() throws Exception {
        Thread caller = Thread.currentThread();
        given(repository.save(any())).willAnswer(invocation -> {
            assertThat(Thread.currentThread()).isSameAs(caller);
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            ConnectionHolder resource = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
            assertThat(resource).isNotNull();
            assertThat(resource.getConnection()).isSameAs(connection);
            return invocation.getArgument(0);
        });

        new TransactionTemplate(transactions).executeWithoutResult(status -> publish());

        verify(repository).save(any());
        verify(dataSource).getConnection();
        verify(connection).commit();
        verify(connection, never()).rollback();
    }

    @Test
    void outboxFailureReachesTheCallerAndRollsBackItsTransaction() throws Exception {
        IllegalStateException failure = new IllegalStateException("forced outbox failure");
        doThrow(failure).when(repository).save(any());

        assertThatThrownBy(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> publish()))
                .isSameAs(failure);
        verify(connection).rollback();
        verify(connection, never()).commit();
    }

    @Test
    void eventOutsideATransactionCannotPersistAnOutbox() {
        assertThatThrownBy(this::publish).isInstanceOf(IllegalTransactionStateException.class);
        verifyNoInteractions(repository);
        verifyNoInteractions(dataSource);
    }

    @Test
    void atomicListenersArePlainSynchronousListenersWithMandatoryPropagation() throws Exception {
        List<Method> listeners = List.of(
                SubscriptionAccountLifecycleService.class.getMethod("onUserWithdrawn", UserWithdrawnEvent.class),
                FeedbackAccountLifecycleService.class.getMethod("onUserWithdrawn", UserWithdrawnEvent.class));
        AnnotationTransactionAttributeSource annotations = new AnnotationTransactionAttributeSource();

        for (Method listener : listeners) {
            assertThat(AnnotatedElementUtils.hasAnnotation(listener, EventListener.class)).isTrue();
            assertThat(AnnotatedElementUtils.hasAnnotation(listener, TransactionalEventListener.class)).isFalse();
            assertThat(AnnotatedElementUtils.hasAnnotation(listener, Async.class)).isFalse();
            assertThat(AnnotatedElementUtils.hasAnnotation(listener.getDeclaringClass(), Async.class)).isFalse();
            assertThat(annotations.getTransactionAttribute(listener, listener.getDeclaringClass()))
                    .isNotNull()
                    .extracting(attribute -> attribute.getPropagationBehavior())
                    .isEqualTo(TransactionDefinition.PROPAGATION_MANDATORY);
        }
    }

    private void publish() {
        events.multicastEvent(new PayloadApplicationEvent<>(this, event));
    }
}
