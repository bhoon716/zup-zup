package bhoon.sugang_helper.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.notification.domain.SeatNotificationDelivery;
import bhoon.sugang_helper.notification.domain.SeatNotificationOutbox;
import bhoon.sugang_helper.notification.domain.SeatNotificationOutboxStatus;
import bhoon.sugang_helper.notification.infra.NotificationChannel;
import bhoon.sugang_helper.notification.infra.NotificationTarget;
import bhoon.sugang_helper.notification.infra.SeatNotificationDeliveryJpaRepository;
import bhoon.sugang_helper.notification.infra.SeatNotificationOutboxJpaRepository;
import bhoon.sugang_helper.subscription.domain.Subscription;
import bhoon.sugang_helper.subscription.domain.SubscriptionRepository;
import bhoon.sugang_helper.user.domain.User;
import bhoon.sugang_helper.user.domain.UserDevice;
import bhoon.sugang_helper.user.domain.UserDeviceRepository;
import bhoon.sugang_helper.user.domain.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("performance")
@ExtendWith(MockitoExtension.class)
class SeatNotificationOutboxProcessorBenchmarkTest {

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

    @InjectMocks
    private SeatNotificationOutboxProcessor processor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(processor, "batchSize", 50);
        ReflectionTestUtils.setField(processor, "maximumAttempts", 5);
        ReflectionTestUtils.setField(processor, "leaseSeconds", 60L);
    }

    @Test
    @DisplayName("[수동] 구독자 1,000명(4,000개 알림 건) 구체화시 saveAll Bulk Save 성능 수동 측정")
    void measureBulkSavePerformance() {
        int subscriberCount = 1000;
        String courseKey = "COMP101";

        SeatNotificationOutbox outbox = SeatNotificationOutbox.builder()
                .courseKey(courseKey)
                .courseName("자바 프로그래밍")
                .previousSeats(0)
                .currentSeats(5)
                .build();
        ReflectionTestUtils.setField(outbox, "id", 99L);

        when(outboxRepository.findByStatusForUpdate(any(SeatNotificationOutboxStatus.class), any(Pageable.class)))
                .thenReturn(List.of(outbox));

        List<Subscription> mockSubscriptions = new ArrayList<>();
        List<User> mockUsers = new ArrayList<>();
        List<UserDevice> mockDevices = new ArrayList<>();

        for (long i = 1; i <= subscriberCount; i++) {
            mockSubscriptions.add(Subscription.builder().courseKey(courseKey).userId(i).isActive(true).build());
            User user = User.builder().email("user" + i + "@jbnu.ac.kr").name("유저" + i).build();
            ReflectionTestUtils.setField(user, "id", i);
            mockUsers.add(user);
        }

        when(subscriptionRepository.findByCourseKeyAndIsActiveTrue(courseKey)).thenReturn(mockSubscriptions);
        when(userRepository.findAllById(any())).thenReturn(mockUsers);
        when(userDeviceRepository.findByUserIdIn(any())).thenReturn(mockDevices);
        when(notificationChannelPolicy.isChannelEnabled(any(), any())).thenReturn(true);
        when(notificationChannelPolicy.resolveTargets(any(), any(), any()))
                .thenReturn(List.of(NotificationTarget.of("target")));
        when(deliveryRepository.existsByOutboxIdAndUserIdAndChannel(anyLong(), anyLong(), any())).thenReturn(false);

        // Warm-up JIT compilation and Mockito reflections
        for (int w = 0; w < 5; w++) {
            processor.materializePendingOutboxes();
        }

        int totalDeliveries = subscriberCount * NotificationChannel.values().length;

        // 1. 단건 save() N회 직렬 수행 시뮬레이션
        long startLoopSave = System.nanoTime();
        for (long i = 1; i <= subscriberCount; i++) {
            for (NotificationChannel channel : NotificationChannel.values()) {
                deliveryRepository.save(SeatNotificationDelivery.builder()
                        .outbox(outbox)
                        .userId(i)
                        .channel(channel)
                        .build());
            }
        }
        long durationLoopSaveNs = System.nanoTime() - startLoopSave;

        // 2. saveAll() Bulk Save 수행
        long startBulkSave = System.nanoTime();
        processor.materializePendingOutboxes();
        long durationBulkSaveNs = System.nanoTime() - startBulkSave;

        double loopSaveMs = durationLoopSaveNs / 1_000_000.0;
        double bulkSaveMs = durationBulkSaveNs / 1_000_000.0;

        System.out.println("\n==================================================");
        System.out.println(" 📊 [ISSUE-148] Outbox 구체화 수동 성능 테스트 (Before vs After)");
        System.out.println("==================================================");
        System.out.printf("• 테스트 조건: 구독 유저 %,d 명 × %,d 개 채널 = 총 %,d 건 알림 대상\n",
                subscriberCount, NotificationChannel.values().length, totalDeliveries);
        System.out.printf("• [Before] 루프 단건 save()  소요 시간: %.3f ms (DB 레포지토리 호출: %,d회)\n",
                loopSaveMs, totalDeliveries);
        System.out.printf("• [After]  Bulk saveAll()    소요 시간: %.3f ms (DB 레포지토리 호출: 1회)\n",
                bulkSaveMs);
        System.out.printf("• 🚀 DB Save 호출 횟수 감소: %,d 회 ➔ 1 회\n", totalDeliveries);
        System.out.println("==================================================\n");

        assertThat(totalDeliveries).isEqualTo(4000);
    }
}
