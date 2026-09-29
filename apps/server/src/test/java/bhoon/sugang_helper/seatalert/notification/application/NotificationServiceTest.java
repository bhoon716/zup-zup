package bhoon.sugang_helper.seatalert.notification.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import bhoon.sugang_helper.common.config.NotificationProperties;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.academic.course.contract.SeatOpenedEvent;
import bhoon.sugang_helper.seatalert.notification.domain.NotificationHistory;
import bhoon.sugang_helper.seatalert.notification.domain.NotificationHistoryRepository;
import bhoon.sugang_helper.seatalert.notification.domain.SeatNotificationOutbox;
import bhoon.sugang_helper.seatalert.contract.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.application.NotificationDeliveryIdempotencyKey;
import bhoon.sugang_helper.seatalert.notification.application.port.NotificationSender;
import bhoon.sugang_helper.seatalert.notification.application.port.NotificationTarget;
import bhoon.sugang_helper.seatalert.notification.application.port.NotificationProviderExecutor;
import bhoon.sugang_helper.seatalert.subscription.contract.SubscriptionRecipientQuery;
import bhoon.sugang_helper.identity.user.domain.DeviceType;
import bhoon.sugang_helper.identity.user.domain.Role;
import bhoon.sugang_helper.identity.user.domain.User;
import bhoon.sugang_helper.identity.user.domain.UserDevice;
import bhoon.sugang_helper.identity.user.contract.NotificationDevice;
import bhoon.sugang_helper.identity.user.contract.NotificationRecipient;
import bhoon.sugang_helper.identity.user.contract.UserNotificationQuery;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // Notification state fixtures intentionally repeat Redis protocol values.
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
    private SubscriptionRecipientQuery subscriptionRecipientQuery;
    @Mock
    private UserNotificationQuery userNotificationQuery;
    @Mock
    private NotificationSender notificationSender;
    @Mock
    private NotificationHistoryRepository notificationHistoryRepository;
    @Mock
    private NotificationProviderExecutor notificationProviderExecutor;

    private NotificationService notificationService;
    private Map<Long, NotificationRecipient> recipients;
    private Map<Long, List<NotificationDevice>> devicesByUser;

    @BeforeEach
    void setUp() {
        recipients = new HashMap<>();
        devicesByUser = new HashMap<>();
        lenient().when(userNotificationQuery.findById(anyLong()))
                .thenAnswer(invocation -> Optional.ofNullable(recipients.get(invocation.getArgument(0))));
        lenient().when(userNotificationQuery.findByIds(anyList()))
                .thenAnswer(invocation -> ((List<Long>) invocation.getArgument(0)).stream()
                        .map(recipients::get).filter(java.util.Objects::nonNull).toList());
        lenient().when(userNotificationQuery.findDevicesByUserId(anyLong()))
                .thenAnswer(invocation -> devicesByUser.getOrDefault(invocation.getArgument(0), List.of()));
        lenient().when(userNotificationQuery.findDevicesByUserIds(anyList()))
                .thenAnswer(invocation -> ((List<Long>) invocation.getArgument(0)).stream()
                        .flatMap(userId -> devicesByUser.getOrDefault(userId, List.of()).stream()).toList());
        // 모든 알림 채널 활성화 상태로 초기화
        NotificationProperties props = new NotificationProperties(true, true, true, true);
        NotificationChannelPolicy notificationChannelPolicy = new NotificationChannelPolicy(props);
        notificationService = new NotificationService(redisService, subscriptionRecipientQuery, userNotificationQuery,
                notificationHistoryRepository, List.of(notificationSender), notificationChannelPolicy,
                notificationProviderExecutor);
        lenient().doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(1)).run();
            return null;
        }).when(notificationProviderExecutor).execute(any(), any());
    }

    @Test
    @DisplayName("중복 알림 방지 키를 원자적으로 선점한 뒤 알림 발송")
    void sendNotificationIfKeyNotExists() {
        // Given
        SeatOpenedEvent event = new SeatOpenedEvent(COURSE_KEY, COURSE_NAME, PROFESSOR, 0, 1);
        String redisKey = "ALERT:" + COURSE_KEY;

        given(redisService.setValuesIfAbsent(eq(redisKey), eq("PENDING"), any(Duration.class))).willReturn(true);
        given(subscriptionRecipientQuery.findActiveRecipientIds(COURSE_KEY)).willReturn(List.of());

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
        verify(subscriptionRecipientQuery, never()).findActiveRecipientIds(anyString());
    }

    @Test
    @DisplayName("한 대상의 발송 실패가 이후 대상 발송을 중단하지 않는다")
    void continuesDeliveryWhenOneTargetFails() {
        // Given
        SeatOpenedEvent event = new SeatOpenedEvent(COURSE_KEY, COURSE_NAME, PROFESSOR, 0, 1);
        String redisKey = "ALERT:" + COURSE_KEY;
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
        given(subscriptionRecipientQuery.findActiveRecipientIds(COURSE_KEY)).willReturn(List.of(1L, 2L));
        addRecipient(user);
        addRecipient(secondUser);
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
        given(subscriptionRecipientQuery.findActiveRecipientIds(anyString())).willReturn(List.of(1L));
        addRecipient(user);
        addDevice(fcmDevice);
        addDevice(webDevice);

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
        addRecipient(user);

        given(notificationSender.supports(NotificationChannel.EMAIL)).willReturn(true);

        // When
        notificationService.sendAdminTestNotification(user.getId(), List.of(NotificationChannel.EMAIL));

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
                .emailEnabled(true)
                .build();
        addRecipient(user);
        when(redisService.setValuesIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        given(notificationSender.supports(NotificationChannel.EMAIL)).willReturn(true);

        // When
        notificationService.sendUserTestNotification(user.getId());

        // Then
        verify(notificationSender, times(1)).send(any(NotificationTarget.class), anyString(), anyString());
        verify(notificationHistoryRepository, never()).save(any(NotificationHistory.class));
    }

    @Test
    @DisplayName("사용자 테스트 알림 - 대상 없는 활성 채널은 건너뛰고 다른 채널은 발송한다")
    void sendUserTestNotification_SkipsChannelWithoutTarget() {
        // Given
        User user = User.builder()
                .id(1L)
                .email(EMAIL)
                .emailEnabled(true)
                .fcmEnabled(true)
                .build();
        addRecipient(user);
        when(redisService.setValuesIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        given(notificationSender.supports(NotificationChannel.EMAIL)).willReturn(true);

        // When
        notificationService.sendUserTestNotification(user.getId());

        // Then
        verify(notificationSender, times(1)).send(any(NotificationTarget.class), anyString(), anyString());
        verify(redisService, never()).deleteValues(anyString());
    }

    @Test
    @DisplayName("사용자 테스트 알림 - 모든 활성 채널에 대상이 없으면 입력 오류를 반환한다")
    void sendUserTestNotification_FailsWhenNoTargetExists() {
        // Given
        User user = User.builder()
                .id(1L)
                .email(EMAIL)
                .fcmEnabled(true)
                .build();
        addRecipient(user);
        when(redisService.setValuesIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> notificationService.sendUserTestNotification(user.getId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
        verify(redisService, times(1)).deleteValues(anyString());
    }

    @Test
    @DisplayName("사용자 테스트 알림 발송 - 쿨타임 중이면 예외 발생")
    void sendUserTestNotification_Cooldown() {
        // Given
        User user = User.builder()
                .id(1L)
                .email(EMAIL)
                .build();
        addRecipient(user);
        when(redisService.setValuesIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> notificationService.sendUserTestNotification(user.getId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("관리자 테스트 알림 발송 - 푸시 (FCM/WEB)")
    void sendTestNotification_Push() {
        // Given
        User user = User.builder().id(1L).email(EMAIL).build();
        UserDevice device = UserDevice.builder().userId(1L).type(DeviceType.FCM).token(DEVICE_TOKEN).build();
        addRecipient(user);
        addDevice(device);

        given(notificationSender.supports(NotificationChannel.FCM)).willReturn(true);

        // When
        notificationService.sendAdminTestNotification(user.getId(), List.of(NotificationChannel.FCM));

        // Then
        verify(notificationSender, times(1)).send(any(NotificationTarget.class), anyString(), anyString());
        verify(notificationHistoryRepository, never()).save(any(NotificationHistory.class));
    }

    @Test
    @DisplayName("관리자 테스트 알림 발송 - 기기 미등록 시 예외 발생")
    void sendTestNotification_Push_NoDevice() {
        // Given
        User user = User.builder().id(1L).email(EMAIL).build();
        addRecipient(user);

        // When & Then
        assertThatThrownBy(
                () -> notificationService.sendAdminTestNotification(user.getId(), List.of(NotificationChannel.WEB)))
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
        addRecipient(user);

        given(notificationSender.supports(NotificationChannel.DISCORD)).willReturn(true);

        // When
        notificationService.sendAdminTestNotification(user.getId(), List.of(NotificationChannel.DISCORD));

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
        addRecipient(user);

        // When & Then
        assertThatThrownBy(() -> notificationService.sendAdminTestNotification(user.getId(),
                List.of(NotificationChannel.DISCORD)))
                .isInstanceOf(CustomException.class)
                .extracting("detail")
                .asString()
                .contains("디스코드 연동 정보가 없습니다");
    }

    @Test
    @DisplayName("durable delivery는 원본 키를 노출하지 않고 provider-safe key를 전파한다")
    void deliversSeatOpeningWithProviderSafeIdempotencyKey() {
        String canonicalKey = "d35e8a1f-3135-4f9b-92b3-635f0c9ea641";
        User user = User.builder()
                .id(1L)
                .email(EMAIL)
                .role(Role.USER)
                .emailEnabled(true)
                .build();
        addRecipient(user);
        SeatNotificationOutbox outbox = SeatNotificationOutbox.builder()
                .courseKey(COURSE_KEY)
                .courseName(COURSE_NAME)
                .professor(PROFESSOR)
                .previousSeats(0)
                .currentSeats(1)
                .build();
        when(notificationSender.supports(NotificationChannel.EMAIL)).thenReturn(true);

        notificationService.deliverSeatOpening(1L, NotificationChannel.EMAIL, outbox, canonicalKey);

        verify(notificationSender).send(any(NotificationTarget.class), anyString(), anyString(),
                eq(NotificationDeliveryIdempotencyKey.providerKey(canonicalKey)));
    }

    private void addRecipient(User user) {
        recipients.put(user.getId(), new NotificationRecipient(
                user.getId(),
                user.isDeleted(),
                user.getEmail(),
                user.getNotificationEmail(),
                user.isEmailEnabled(),
                user.isWebPushEnabled(),
                user.isFcmEnabled(),
                user.isDiscordEnabled(),
                user.getDiscordId()));
    }

    private void addDevice(UserDevice device) {
        NotificationDevice.Type type = device.getType() == DeviceType.WEB
                ? NotificationDevice.Type.WEB : NotificationDevice.Type.FCM;
        devicesByUser.computeIfAbsent(device.getUserId(), ignored -> new java.util.ArrayList<>())
                .add(new NotificationDevice(device.getUserId(), type, device.getToken(),
                        device.getP256dh(), device.getAuth()));
    }
}
