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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(redisService, subscriptionRepository, userRepository,
                userDeviceRepository, notificationHistoryRepository, List.of(notificationSender));
    }

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
        User user = User.builder()
                .id(1L)
                .name("Tester")
                .email("test@example.com")
                .role(Role.USER)
                .emailEnabled(true)
                .webPushEnabled(true)
                .fcmEnabled(true)
                .build();

        UserDevice fcmDevice = UserDevice.builder()
                .userId(1L).type(DeviceType.FCM).token("fcm-token").build();
        UserDevice webDevice = UserDevice.builder()
                .userId(1L).type(DeviceType.WEB).token("web-endpoint").p256dh("p256").auth("auth").build();

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
        verify(notificationHistoryRepository, times(3)).save(any(NotificationHistory.class));
    }

    @Test
    @DisplayName("관리자 테스트 알림 발송 - 이메일")
    void sendTestNotification_Email() {
        // Given
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .build();
        List<NotificationChannel> channels = List.of(NotificationChannel.EMAIL);

        given(notificationSender.supports(NotificationChannel.EMAIL)).willReturn(true);

        // When
        notificationService.sendTestNotification(user, channels);

        // Then
        verify(notificationSender, times(1)).send(any(NotificationTarget.class), anyString(), anyString());
        verify(notificationHistoryRepository, times(1)).save(any(NotificationHistory.class));
    }

    @Test
    @DisplayName("관리자 테스트 알림 발송 - 푸시 (FCM/WEB)")
    void sendTestNotification_Push() {
        // Given
        User user = User.builder().id(1L).email("test@example.com").build();
        List<NotificationChannel> channels = List.of(NotificationChannel.FCM);
        UserDevice device = UserDevice.builder().userId(1L).type(DeviceType.FCM).token("token").build();

        given(userDeviceRepository.findByUserIdAndType(1L, DeviceType.FCM)).willReturn(List.of(device));
        given(notificationSender.supports(NotificationChannel.FCM)).willReturn(true);

        // When
        notificationService.sendTestNotification(user, channels);

        // Then
        verify(notificationSender, times(1)).send(any(NotificationTarget.class), anyString(), anyString());
        verify(notificationHistoryRepository, times(1)).save(any(NotificationHistory.class));
    }

    @Test
    @DisplayName("관리자 테스트 알림 발송 - 기기 미등록 시 예외 발생")
    void sendTestNotification_Push_NoDevice() {
        // Given
        User user = User.builder().id(1L).email("test@example.com").build();
        List<NotificationChannel> channels = List.of(NotificationChannel.WEB);

        given(userDeviceRepository.findByUserIdAndType(1L, DeviceType.WEB)).willReturn(List.of());

        // When & Then
        // When & Then
        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> notificationService.sendTestNotification(user, channels))
                .isInstanceOf(bhoon.sugang_helper.common.error.CustomException.class)
                .extracting("detail")
                .asString()
                .contains("등록된 웹 푸시 기기가 없습니다");
    }
}
