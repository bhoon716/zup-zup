package bhoon.sugang_helper.seatalert.notification.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.enums.SeatNotificationDeliveryStatus;
import bhoon.sugang_helper.seatalert.notification.enums.SeatNotificationOutboxStatus;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationDeliveryRepository;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationOutboxRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SeatNotificationBacklogMonitorTest {

    private static final String CHANNEL_TAG = "channel";
    private static final String EMAIL_CHANNEL = "EMAIL";
    private static final String DEPTH = "notification.outbox.queue.depth";
    private static final String LAST_SUCCESS = "notification.outbox.monitor.last.success.timestamp.seconds";
    private static final String HEALTHY = "notification.outbox.monitor.healthy";

    @Mock
    private SeatNotificationDeliveryRepository deliveryRepository;

    @Mock
    private SeatNotificationOutboxRepository outboxRepository;

    private SimpleMeterRegistry registry;
    private SeatNotificationBacklogMonitor monitor;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        monitor = new SeatNotificationBacklogMonitor(deliveryRepository, outboxRepository, registry);
    }

    @Test
    @DisplayName("최초 DB 확인 전에는 빈 대기열을 정상으로 표시하지 않는다")
    void startsUnknownUntilTheDatabaseHasBeenRead() {
        // when & then
        assertThat(registry.get(DEPTH).tag(CHANNEL_TAG, EMAIL_CHANNEL).gauge().value()).isEqualTo(-1);
        assertThat(registry.get(HEALTHY).gauge().value()).isZero();
        assertThat(registry.get(LAST_SUCCESS).gauge().value()).isZero();
    }

    @Test
    @DisplayName("실제 DB 대기 건수와 가장 오래된 준비 시각을 채널별로 노출한다")
    void publishesReadyBacklogAndOldestTimestampByChannel() {
        // given
        var backlog = mock(SeatNotificationDeliveryRepository.ReadyBacklog.class);
        LocalDateTime readyAt = LocalDateTime.now().minusMinutes(12);
        when(backlog.getChannel()).thenReturn(NotificationChannel.EMAIL);
        when(backlog.getDepth()).thenReturn(42L);
        when(backlog.getOldestReadyAt()).thenReturn(readyAt);
        when(deliveryRepository.findReadyBacklog(eq(SeatNotificationDeliveryStatus.PENDING),
                eq(SeatNotificationDeliveryStatus.PROCESSING), any())).thenReturn(List.of(backlog));
        when(outboxRepository.countByStatus(SeatNotificationOutboxStatus.PENDING)).thenReturn(7L);

        // when
        monitor.refresh();

        // then
        assertThat(registry.get(DEPTH).tag(CHANNEL_TAG, EMAIL_CHANNEL).gauge().value()).isEqualTo(42);
        assertThat(registry.get(DEPTH).tag(CHANNEL_TAG, "WEB").gauge().value()).isZero();
        assertThat(registry.get("notification.outbox.oldest.ready.timestamp.seconds")
                .tag(CHANNEL_TAG, EMAIL_CHANNEL).gauge().value())
                .isEqualTo(readyAt.atZone(ZoneId.systemDefault()).toEpochSecond());
        assertThat(registry.get("notification.outbox.materialization.pending").gauge().value()).isEqualTo(7);
        assertThat(registry.get(LAST_SUCCESS).gauge().value()).isPositive();
        assertThat(registry.get(HEALTHY).gauge().value()).isEqualTo(1);
    }

    @Test
    @DisplayName("DB 조회 실패는 이전 숫자를 무효화하고 마지막 성공 시각은 보존한다")
    void invalidatesOldValuesWhenEitherDatabaseReadFails() {
        // given
        when(deliveryRepository.findReadyBacklog(any(), any(), any())).thenReturn(List.of());
        when(outboxRepository.countByStatus(SeatNotificationOutboxStatus.PENDING))
                .thenReturn(0L).thenThrow(new IllegalStateException("password=private-secret"));
        monitor.refresh();
        double checkedAt = registry.get(LAST_SUCCESS).gauge().value();

        // when
        monitor.refresh();

        // then
        assertThat(registry.get(DEPTH).tag(CHANNEL_TAG, EMAIL_CHANNEL).gauge().value()).isEqualTo(-1);
        assertThat(registry.get("notification.outbox.materialization.pending").gauge().value()).isEqualTo(-1);
        assertThat(registry.get(HEALTHY).gauge().value()).isZero();
        assertThat(registry.get(LAST_SUCCESS).gauge().value()).isEqualTo(checkedAt);
    }
}
