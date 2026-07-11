package bhoon.sugang_helper.notification.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.common.config.NotificationProperties;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.course.domain.SeatOpenedEvent;
import bhoon.sugang_helper.notification.domain.NotificationHistory;
import bhoon.sugang_helper.notification.domain.NotificationHistoryRepository;
import bhoon.sugang_helper.notification.infra.NotificationChannel;
import bhoon.sugang_helper.notification.infra.NotificationSender;
import bhoon.sugang_helper.notification.infra.NotificationTarget;
import bhoon.sugang_helper.subscription.domain.Subscription;
import bhoon.sugang_helper.subscription.domain.SubscriptionRepository;
import bhoon.sugang_helper.user.domain.DeviceType;
import bhoon.sugang_helper.user.domain.Role;
import bhoon.sugang_helper.user.domain.User;
import bhoon.sugang_helper.user.domain.UserDevice;
import bhoon.sugang_helper.user.domain.UserDeviceRepository;
import bhoon.sugang_helper.user.domain.UserRepository;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final String EMAIL = "test@example.com";
    private static final String COURSE_KEY = "12345-01";
    private static final String COURSE_NAME = "Test Course";
    private static final String PROFESSOR = "Test Professor";
    private static final String USER_NAME = "Tester";
    private static final String DEVICE_TOKEN = "token";

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
        // 모든 알림 채널 활성화 상태로 초기화
        NotificationProperties props = new NotificationProperties(true, true, true, true);
        NotificationChannelPolicy notificationChannelPolicy = new NotificationChannelPolicy(props);
        notificationService = new NotificationService(redisService, subscriptionRepository, userRepository,
                userDeviceRepository, notificationHistoryRepository, List.of(notificationSender),
                notificationChannelPolicy);
    }

    @Test
    @DisplayName("중복 알림 방지 키를 원자적으로 선점한 뒤 알림 발송")
    void sendNotificationIfKeyNotExists() {
        // Given
        SeatOpenedEvent event = new SeatOpenedEvent(COURSE_KEY, COURSE_NAME, PROFESSOR, 0, 1);
        String redisKey = "ALERT:" + COURSE_KEY;

        given(redisService.setValuesIfAbsent(eq(redisKey), eq("PENDING"), any(Duration.class))).willReturn(true);
        given(subscriptionRepository.findByCourseKeyAndIsActiveTrue(COURSE_KEY)).willReturn(List.of());

        // When
        notificationService.handleSeatOpenedEvent(event);

        // Then
        verify(redisService, times(1)).setValuesIfAbsent(eq(redisKey), eq("PENDING"), any(Duration.class));
        verify(redisService, times(1)).setValues(eq(redisKey), eq("SENT"), any(Duration.class));
    }

    @Test
    @DisplayName("중복 알림 방지 키를 선점하지 못하면 알림 발송을 건너뜀")
    void skipNotificationIfKeyExists() {
        // Given
        SeatOpenedEvent event = new SeatOpenedEvent(COURSE_KEY, COURSE_NAME, PROFESSOR, 0, 1);
        String redisKey = "ALERT:" + COURSE_KEY;

        given(redisService.setValuesIfAbsent(eq(redisKey), eq("PENDING"), any(Duration.class))).willReturn(false);

        // When
        notificationService.handleSeatOpenedEvent(event);

        // Then
        verify(redisService, times(1)).setValuesIfAbsent(eq(redisKey), eq("PENDING"), any(Duration.class));
        verify(redisService, never()).setValues(anyString(), anyString(), any(Duration.class));
        verify(redisService, never()).deleteValues(anyString());
        verify(subscriptionRepository, never()).findByCourseKeyAndIsActiveTrue(anyString());
    }

    @Test
    @DisplayName("한 대상의 발송 실패가 이후 대상 발송을 중단하지 않는다")
    void continuesDeliveryWhenOneTargetFails() {
        // Given
        SeatOpenedEvent event = new SeatOpenedEvent(COURSE_KEY, COURSE_NAME, PROFESSOR, 0, 1);
        String redisKey = "ALERT:" + COURSE_KEY;
        Subscription firstSubscription = Subscription.builder().userId(1L).courseKey(COURSE_KEY).isActive(true)
                .build();
        Subscription secondSubscription = Subscription.builder().userId(2L).courseKey(COURSE_KEY).isActive(true)
                .build();
        User user = User.builder()
                .id(1L)
                .name(USER_NAME)
                .email(EMAIL)
                .role(Role.USER)
                .emailEnabled(true)
                .build();
        User secondUser = User.builder().id(2L).name("Second").email("second@example.com").role(Role.USER)
                .emailEnabled(true).build();

        given(redisService.setValuesIfAbsent(eq(redisKey), eq("PENDING"), any(Duration.class))).willReturn(true);
        given(subscriptionRepository.findByCourseKeyAndIsActiveTrue(COURSE_KEY))
                .willReturn(List.of(firstSubscription, secondSubscription));
        given(userRepository.findAllById(anyList())).willReturn(List.of(user, secondUser));
        given(userDeviceRepository.findByUserIdIn(anyList())).willReturn(List.of());
        given(notificationSender.supports(NotificationChannel.EMAIL)).willReturn(true);
        doThrow(new RuntimeException("boom")).doNothing().when(notificationSender)
                .send(any(), anyString(), anyString());

        // When
        notificationService.handleSeatOpenedEvent(event);

        verify(redisService, times(1)).setValuesIfAbsent(eq(redisKey), eq("PENDING"), any(Duration.class));
        verify(redisService, never()).deleteValues(anyString());
        verify(notificationSender, times(2)).send(any(), anyString(), anyString());
        verify(redisService, times(1)).setValues(eq(redisKey), eq("SENT"), any(Duration.class));
    }

    @Test
    @DisplayName("구독자에게 멀티 채널(Email, FCM, WEB, Discord) 발송 검증")
    void dispatchToMultiChannels() {
        // Given
        SeatOpenedEvent event = new SeatOpenedEvent(COURSE_KEY, COURSE_NAME, PROFESSOR, 0, 1);
        Subscription subscription = Subscription.builder().userId(1L).courseKey(COURSE_KEY).isActive(true)
                .build();
        User user = User.builder()
                .id(1L)
                .name(USER_NAME)
                .email(EMAIL)
                .role(Role.USER)
                .emailEnabled(true)
                .webPushEnabled(true)
                .fcmEnabled(true)
                .discordEnabled(true)
                .discordId("discord-id")
                .build();

        UserDevice fcmDevice = UserDevice.builder()
                .userId(1L).type(DeviceType.FCM).token("fcm-token").build();
        UserDevice webDevice = UserDevice.builder()
                .userId(1L).type(DeviceType.WEB).token("web-endpoint").p256dh("p256").auth("auth")
                .build();

        given(redisService.setValuesIfAbsent(eq("ALERT:" + COURSE_KEY), eq("PENDING"), any(Duration.class)))
                .willReturn(true);
        given(subscriptionRepository.findByCourseKeyAndIsActiveTrue(anyString()))
                .willReturn(List.of(subscription));
        given(userRepository.findAllById(anyList())).willReturn(List.of(user));
        given(userDeviceRepository.findByUserIdIn(anyList())).willReturn(List.of(fcmDevice, webDevice));

        given(notificationSender.supports(NotificationChannel.EMAIL)).willReturn(true);
        given(notificationSender.supports(NotificationChannel.FCM)).willReturn(true);
        given(notificationSender.supports(NotificationChannel.WEB)).willReturn(true);
        given(notificationSender.supports(NotificationChannel.DISCORD)).willReturn(true);

        // When
        notificationService.handleSeatOpenedEvent(event);

        // Then
        verify(redisService, times(1)).setValuesIfAbsent(eq("ALERT:" + COURSE_KEY), eq("PENDING"),
                any(Duration.class));
        verify(redisService, times(1)).setValues(eq("ALERT:" + COURSE_KEY), eq("SENT"), any(Duration.class));
        verify(notificationHistoryRepository, times(4)).save(any(NotificationHistory.class));
    }

    @Test
    @DisplayName("관리자 테스트 알림 발송 - 이메일")
    void sendTestNotification_Email() {
        // Given
        User user = User.builder()
                .id(1L)
                .email(EMAIL)
                .build();

        given(notificationSender.supports(NotificationChannel.EMAIL)).willReturn(true);

        // When
        notificationService.sendTestNotification(user, List.of(NotificationChannel.EMAIL));

        // Then
        verify(notificationSender, times(1)).send(any(NotificationTarget.class), anyString(), anyString());
        verify(notificationHistoryRepository, never()).save(any(NotificationHistory.class));
    }

    @Test
    @DisplayName("사용자 테스트 알림 발송 - 쿨타임 획득 시 정상 발송")
    void sendUserTestNotification_Success() {
        // Given
        User user = User.builder()
                .id(1L)
                .email(EMAIL)
                .build();
        when(redisService.setValuesIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        given(notificationSender.supports(NotificationChannel.EMAIL)).willReturn(true);

        // When
        notificationService.sendUserTestNotification(user, List.of(NotificationChannel.EMAIL));

        // Then
        verify(notificationSender, times(1)).send(any(NotificationTarget.class), anyString(), anyString());
        verify(notificationHistoryRepository, never()).save(any(NotificationHistory.class));
    }

    @Test
    @DisplayName("사용자 테스트 알림 발송 - 쿨타임 중이면 예외 발생")
    void sendUserTestNotification_Cooldown() {
        // Given
        User user = User.builder()
                .id(1L)
                .email(EMAIL)
                .build();
        when(redisService.setValuesIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> notificationService.sendUserTestNotification(user,
                List.of(NotificationChannel.EMAIL)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("관리자 테스트 알림 발송 - 푸시 (FCM/WEB)")
    void sendTestNotification_Push() {
        // Given
        User user = User.builder().id(1L).email(EMAIL).build();
        UserDevice device = UserDevice.builder().userId(1L).type(DeviceType.FCM).token(DEVICE_TOKEN).build();

        given(userDeviceRepository.findByUserId(1L)).willReturn(List.of(device));
        given(notificationSender.supports(NotificationChannel.FCM)).willReturn(true);

        // When
        notificationService.sendTestNotification(user, List.of(NotificationChannel.FCM));

        // Then
        verify(notificationSender, times(1)).send(any(NotificationTarget.class), anyString(), anyString());
        verify(notificationHistoryRepository, never()).save(any(NotificationHistory.class));
    }

    @Test
    @DisplayName("관리자 테스트 알림 발송 - 기기 미등록 시 예외 발생")
    void sendTestNotification_Push_NoDevice() {
        // Given
        User user = User.builder().id(1L).email(EMAIL).build();

        given(userDeviceRepository.findByUserId(1L)).willReturn(List.of());

        // When & Then
        assertThatThrownBy(
                () -> notificationService.sendTestNotification(user, List.of(NotificationChannel.WEB)))
                .isInstanceOf(CustomException.class)
                .extracting("detail")
                .asString()
                .contains("등록된 웹 푸시 기기가 없습니다");
    }

    @Test
    @DisplayName("관리자 테스트 알림 발송 - 디스코드")
    void sendTestNotification_Discord() {
        // Given
        User user = User.builder()
                .id(1L)
                .email(EMAIL)
                .discordId("discord-id")
                .build();

        given(notificationSender.supports(NotificationChannel.DISCORD)).willReturn(true);

        // When
        notificationService.sendTestNotification(user, List.of(NotificationChannel.DISCORD));

        // Then
        verify(notificationSender, times(1)).send(any(NotificationTarget.class), anyString(), anyString());
        verify(notificationHistoryRepository, never()).save(any(NotificationHistory.class));
    }

    @Test
    @DisplayName("관리자 테스트 알림 발송 - 디스코드 미연동 시 예외 발생")
    void sendTestNotification_Discord_NotLinked() {
        // Given
        User user = User.builder()
                .id(1L)
                .email(EMAIL)
                .discordId(null)
                .build();

        // When & Then
        assertThatThrownBy(() -> notificationService.sendTestNotification(user,
                List.of(NotificationChannel.DISCORD)))
                .isInstanceOf(CustomException.class)
                .extracting("detail")
                .asString()
                .contains("디스코드 연동 정보가 없습니다");
    }
}
