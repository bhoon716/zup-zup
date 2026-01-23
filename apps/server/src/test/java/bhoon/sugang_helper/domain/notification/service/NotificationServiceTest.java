package bhoon.sugang_helper.domain.notification.service;

import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.domain.course.event.SeatOpenedEvent;
import bhoon.sugang_helper.domain.notification.entity.NotificationHistory;
import bhoon.sugang_helper.domain.notification.repository.NotificationHistoryRepository;
import bhoon.sugang_helper.domain.notification.sender.NotificationChannel;
import bhoon.sugang_helper.domain.notification.sender.NotificationSender;
import bhoon.sugang_helper.domain.notification.sender.NotificationTarget;
import bhoon.sugang_helper.domain.subscription.entity.Subscription;
import bhoon.sugang_helper.domain.subscription.repository.SubscriptionRepository;
import bhoon.sugang_helper.domain.user.entity.Role;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.entity.UserDevice;
import bhoon.sugang_helper.domain.user.enums.DeviceType;
import bhoon.sugang_helper.domain.user.repository.UserDeviceRepository;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private RedisService redisService;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationSender notificationSender;
    @Mock
    private NotificationHistoryRepository notificationHistoryRepository;
    @Mock
    private UserDeviceRepository userDeviceRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("중복 알림 방지 키가 없을 때 알림 발송")
    void sendNotificationIfKeyNotExists() {
        // Given
        SeatOpenedEvent event = new SeatOpenedEvent("12345-01", "Test Course", 0, 1);
        String redisKey = "ALERT:12345-01";

        given(redisService.hasKey(redisKey)).willReturn(false);
        given(subscriptionRepository.findByCourseKeyAndIsActiveTrue("12345-01")).willReturn(List.of());

        // When
        notificationService.handleSeatOpenedEvent(event);

        // Then
        verify(redisService, times(1)).setValues(eq(redisKey), eq("SENT"), any(Duration.class));
    }

    @Test
    @DisplayName("중복 알림 방지 키가 있을 때 알림 발송 건너뜀")
    void skipNotificationIfKeyExists() {
        // Given
        SeatOpenedEvent event = new SeatOpenedEvent("12345-01", "Test Course", 0, 1);
        String redisKey = "ALERT:12345-01";

        given(redisService.hasKey(redisKey)).willReturn(true);

        // When
        notificationService.handleSeatOpenedEvent(event);

        // Then
        verify(redisService, never()).setValues(anyString(), anyString(), any(Duration.class));
        verify(subscriptionRepository, never()).findByCourseKeyAndIsActiveTrue(anyString());
    }

    @Test
    @DisplayName("구독자에게 멀티 채널(Email, FCM, WEB) 발송 검증")
    void dispatchToMultiChannels() {
        // Given
        SeatOpenedEvent event = new SeatOpenedEvent("12345-01", "Test Course", 0, 1);
        Subscription subscription = Subscription.builder().userId(1L).courseKey("12345-01").isActive(true).build();
        User user = User.builder().id(1L).name("Tester").email("test@example.com").role(Role.USER).build();

        UserDevice fcmDevice = UserDevice.builder()
                .userId(1L).type(DeviceType.FCM).token("fcm-token").build();
        UserDevice webDevice = UserDevice.builder()
                .userId(1L).type(DeviceType.WEB).token("web-endpoint").p256dh("p256").auth("auth").build();

        notificationService = new NotificationService(redisService, subscriptionRepository, userRepository,
                userDeviceRepository, notificationHistoryRepository, List.of(notificationSender));

        given(redisService.hasKey(anyString())).willReturn(false);
        given(subscriptionRepository.findByCourseKeyAndIsActiveTrue(anyString())).willReturn(List.of(subscription));
        given(userRepository.findAllById(anyList())).willReturn(List.of(user));
        given(userDeviceRepository.findByUserIdIn(anyList())).willReturn(List.of(fcmDevice, webDevice));

        given(notificationSender.supports(NotificationChannel.EMAIL)).willReturn(true);
        given(notificationSender.supports(NotificationChannel.FCM)).willReturn(true);
        given(notificationSender.supports(NotificationChannel.WEB)).willReturn(true);

        // When
        notificationService.handleSeatOpenedEvent(event);

        // Then
        verify(notificationSender, times(3)).send(any(NotificationTarget.class), anyString(), anyString());
        verify(notificationHistoryRepository, times(3)).save(any(NotificationHistory.class));
    }
}
