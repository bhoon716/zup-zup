package bhoon.sugang_helper.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.notification.domain.SeatNotificationDelivery;
import bhoon.sugang_helper.notification.domain.SeatNotificationOutbox;
import bhoon.sugang_helper.notification.infra.NotificationChannel;
import bhoon.sugang_helper.notification.infra.SeatNotificationDeliveryJpaRepository;
import bhoon.sugang_helper.notification.infra.SeatNotificationOutboxJpaRepository;
import bhoon.sugang_helper.subscription.domain.SubscriptionRepository;
import bhoon.sugang_helper.user.domain.UserDeviceRepository;
import bhoon.sugang_helper.user.domain.UserRepository;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SeatNotificationOutboxProcessorTest {

    @Mock
    private SeatNotificationOutboxJpaRepository outboxRepository;
    @Mock
    private SeatNotificationDeliveryJpaRepository deliveryRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserDeviceRepository userDeviceRepository;
    @Mock
    private NotificationChannelPolicy notificationChannelPolicy;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SeatNotificationDeliverySettlementService settlementService;
    @Mock
    private MeterRegistry meterRegistry;
    @Mock
    private Counter counter;
    @InjectMocks
    private SeatNotificationOutboxProcessor processor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(processor, "maximumAttempts", 3);
        ReflectionTestUtils.setField(processor, "batchSize", 10);
        ReflectionTestUtils.setField(processor, "leaseSeconds", 60L);
        when(meterRegistry.counter(anyString(), eq("channel"), eq("EMAIL"))).thenReturn(counter);
    }

    @Test
    void failedChannelIsRetriedWithoutMarkingTheOutboxComplete() {
        String secret = "fcm-token-should-not-appear";
        SeatNotificationDelivery delivery = processingDelivery();
        SeatNotificationDeliveryClaim claim = new SeatNotificationDeliveryClaim(1L, "claim-token");
        when(settlementService.loadForDispatch(claim)).thenReturn(dispatch(delivery));
        when(settlementService.markFailure(eq(claim), anyString(), eq(3)))
                .thenReturn(new SeatNotificationDeliveryFailureResult(true, false, 1));
        org.mockito.Mockito.doThrow(new RuntimeException(secret))
                .when(notificationService).deliverSeatOpening(eq(1L), eq(NotificationChannel.EMAIL),
                        eq(delivery.getOutbox()), anyString());
        Logger logger = (Logger) LoggerFactory.getLogger(SeatNotificationOutboxProcessor.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            processor.processDelivery(claim);

            String messages = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .collect(Collectors.joining("\n"));
            assertThat(messages).doesNotContain(secret).contains("failureCode=UNEXPECTED");
            verify(counter).increment();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void maximumFailuresMoveOnlyThatChannelToDlq() {
        SeatNotificationDelivery delivery = processingDelivery();
        SeatNotificationDeliveryClaim claim = new SeatNotificationDeliveryClaim(1L, "claim-token");
        when(settlementService.loadForDispatch(claim)).thenReturn(dispatch(delivery));
        when(settlementService.markFailure(eq(claim), anyString(), eq(3)))
                .thenReturn(new SeatNotificationDeliveryFailureResult(true, true, 3));
        org.mockito.Mockito.doThrow(new RuntimeException("mail unavailable"))
                .when(notificationService).deliverSeatOpening(eq(1L), eq(NotificationChannel.EMAIL),
                        eq(delivery.getOutbox()), anyString());

        processor.processDelivery(claim);

        verify(counter).increment();
    }

    private SeatNotificationDelivery processingDelivery() {
        SeatNotificationOutbox outbox = SeatNotificationOutbox.builder()
                .courseKey("course-key")
                .courseName("Course")
                .previousSeats(0)
                .currentSeats(1)
                .build();
        ReflectionTestUtils.setField(outbox, "id", 10L);
        SeatNotificationDelivery delivery = SeatNotificationDelivery.builder()
                .outbox(outbox)
                .userId(1L)
                .channel(NotificationChannel.EMAIL)
                .build();
        ReflectionTestUtils.setField(delivery, "id", 1L);
        delivery.claim(LocalDateTime.now().plusMinutes(1));
        return delivery;
    }

    private SeatNotificationDeliveryDispatch dispatch(SeatNotificationDelivery delivery) {
        return new SeatNotificationDeliveryDispatch(delivery.getUserId(), delivery.getChannel(),
                delivery.getOutbox(), delivery.getIdempotencyKey());
    }
}
