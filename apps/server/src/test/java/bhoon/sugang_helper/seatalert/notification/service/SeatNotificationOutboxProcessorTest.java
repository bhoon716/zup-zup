package bhoon.sugang_helper.seatalert.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.identity.user.service.UserNotificationQueryService;
import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.dto.result.SeatNotificationDeliveryClaim;
import bhoon.sugang_helper.seatalert.notification.dto.result.SeatNotificationDeliveryDispatch;
import bhoon.sugang_helper.seatalert.notification.dto.result.SeatNotificationDeliveryFailureResult;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationDelivery;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationOutbox;
import bhoon.sugang_helper.seatalert.notification.exception.NotificationProviderException;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationDeliveryRepository;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationOutboxRepository;
import bhoon.sugang_helper.seatalert.subscription.service.SubscriptionQueryService;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SeatNotificationOutboxProcessorTest {

    @Mock
    private SeatNotificationOutboxRepository outboxRepository;

    @Mock
    private SeatNotificationDeliveryRepository deliveryRepository;

    @Mock
    private SubscriptionQueryService subscriptionQueryService;

    @Mock
    private UserNotificationQueryService userNotificationQuery;

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
        Mockito.lenient().when(meterRegistry.counter(anyString(), eq("channel"), eq("EMAIL")))
                .thenReturn(counter);
    }

    @Test
    @DisplayName("채널 실패는 Outbox를 완료하지 않고 재시도한다")
    void failedChannelIsRetriedWithoutMarkingTheOutboxComplete() {
        // given
        String secret = "device-token-should-not-appear";
        SeatNotificationDelivery delivery = processingDelivery();
        SeatNotificationDeliveryClaim claim = new SeatNotificationDeliveryClaim(1L, "claim-token");
        when(settlementService.loadForDispatch(claim)).thenReturn(dispatch(delivery));
        when(settlementService.markFailure(eq(claim), anyString(), eq(3)))
                .thenReturn(new SeatNotificationDeliveryFailureResult(true, false, 1));
        Mockito.doThrow(new RuntimeException(secret))
                .when(notificationService).deliverSeatOpening(eq(1L), eq(NotificationChannel.EMAIL),
                        eq(delivery.getOutbox()), anyString());
        Logger logger = (Logger) LoggerFactory.getLogger(SeatNotificationOutboxProcessor.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        // when & then
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
    @DisplayName("최대 실패 횟수에 도달한 채널만 DLQ로 이동한다")
    void maximumFailuresMoveOnlyThatChannelToDlq() {
        // given
        SeatNotificationDelivery delivery = processingDelivery();
        SeatNotificationDeliveryClaim claim = new SeatNotificationDeliveryClaim(1L, "claim-token");
        when(settlementService.loadForDispatch(claim)).thenReturn(dispatch(delivery));
        when(settlementService.markFailure(eq(claim), anyString(), eq(3)))
                .thenReturn(new SeatNotificationDeliveryFailureResult(true, true, 3));
        Mockito.doThrow(new RuntimeException("mail unavailable"))
                .when(notificationService).deliverSeatOpening(eq(1L), eq(NotificationChannel.EMAIL),
                        eq(delivery.getOutbox()), anyString());

        // when
        processor.processDelivery(claim);

        // then
        verify(counter).increment();
    }

    @Test
    @DisplayName("제공자 영구 오류는 재시도 없이 발송을 DLQ로 이동한다")
    void permanentProviderFailureMovesDeliveryToDlqWithoutRetryBudget() {
        // given
        SeatNotificationDelivery delivery = processingDelivery();
        SeatNotificationDeliveryClaim claim = new SeatNotificationDeliveryClaim(1L, "claim-token");
        when(settlementService.loadForDispatch(claim)).thenReturn(dispatch(delivery));
        when(settlementService.markFailure(eq(claim), anyString(), eq(1)))
                .thenReturn(new SeatNotificationDeliveryFailureResult(true, true, 1));
        Mockito.doThrow(new NotificationProviderException(
                        ErrorCode.WEB_PUSH_INVALID_SUBSCRIPTION, false, "INVALID_RECIPIENT", 404, null))
                .when(notificationService).deliverSeatOpening(eq(1L), eq(NotificationChannel.EMAIL),
                        eq(delivery.getOutbox()), anyString());

        // when
        processor.processDelivery(claim);

        // then
        verify(settlementService).markFailure(eq(claim), anyString(), eq(1));
        verify(counter).increment();
    }

    @Test
    @DisplayName("발송 선점은 제한된 크기의 FIFO 페이지를 사용한다")
    void claimReadyDeliveriesUsesBoundedFifoPageSize() {
        // given
        Mockito.doReturn(List.of()).when(deliveryRepository).findReadyForUpdate(
                ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(Pageable.class));

        // when
        processor.claimReadyDeliveries(4);

        // then
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(deliveryRepository).findReadyForUpdate(
                ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any(), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(4);
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
