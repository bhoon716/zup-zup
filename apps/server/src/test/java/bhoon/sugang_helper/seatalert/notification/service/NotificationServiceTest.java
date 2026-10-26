package bhoon.sugang_helper.seatalert.notification.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.common.config.NotificationProperties;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.identity.user.dto.NotificationDevice;
import bhoon.sugang_helper.identity.user.dto.NotificationRecipient;
import bhoon.sugang_helper.identity.user.entity.User;
import bhoon.sugang_helper.identity.user.entity.UserDevice;
import bhoon.sugang_helper.identity.user.enums.DeviceType;
import bhoon.sugang_helper.identity.user.enums.NotificationDeviceType;
import bhoon.sugang_helper.identity.user.enums.Role;
import bhoon.sugang_helper.identity.user.service.UserNotificationQuery;
import bhoon.sugang_helper.seatalert.enums.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.client.NotificationProviderResilience;
import bhoon.sugang_helper.seatalert.notification.client.NotificationSender;
import bhoon.sugang_helper.seatalert.notification.dto.NotificationTarget;
import bhoon.sugang_helper.seatalert.notification.entity.NotificationHistory;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationOutbox;
import bhoon.sugang_helper.seatalert.notification.repository.NotificationHistoryRepository;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static final String DEVICE_TOKEN = "token";

    @Mock
    private RedisService redisService;
    @Mock
    private UserNotificationQuery userNotificationQuery;
    @Mock
    private NotificationSender notificationSender;
    @Mock
    private NotificationHistoryRepository notificationHistoryRepository;
    @Mock
    private NotificationProviderResilience notificationProviderResilience;

    private NotificationService notificationService;
    private Map<Long, NotificationRecipient> recipients;
    private Map<Long, List<NotificationDevice>> devicesByUser;

    @BeforeEach
    void setUp() {
        recipients = new HashMap<>();
        devicesByUser = new HashMap<>();
        lenient().when(userNotificationQuery.findById(anyLong()))
                .thenAnswer(invocation -> Optional.ofNullable(recipients.get(invocation.getArgument(0))));
        lenient().when(userNotificationQuery.findDevicesByUserId(anyLong()))
                .thenAnswer(invocation -> devicesByUser.getOrDefault(invocation.getArgument(0), List.of()));
        // 모든 알림 채널 활성화 상태로 초기화
        NotificationProperties props = new NotificationProperties(true, true, true, true);
        NotificationChannelPolicy notificationChannelPolicy = new NotificationChannelPolicy(props);
        notificationService = new NotificationService(redisService, userNotificationQuery,
                notificationHistoryRepository, List.of(notificationSender), notificationChannelPolicy,
                notificationProviderResilience);
        lenient().doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(1)).run();
            return null;
        }).when(notificationProviderResilience).execute(any(), any());
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
    @DisplayName("사용자 테스트 알림의 제공자 실패는 전파하고 재시도할 수 있게 쿨타임을 해제한다")
    void sendUserTestNotification_ReleasesCooldownOnProviderFailure() {
        // Given
        User user = User.builder().id(1L).email(EMAIL).emailEnabled(true).build();
        addRecipient(user);
        given(redisService.setValuesIfAbsent(anyString(), anyString(), any(Duration.class))).willReturn(true);
        given(notificationSender.supports(NotificationChannel.EMAIL)).willReturn(true);
        RuntimeException failure = new RuntimeException("provider unavailable");
        doThrow(failure).when(notificationSender).send(any(), anyString(), anyString());

        // When & Then
        assertThatThrownBy(() -> notificationService.sendUserTestNotification(user.getId())).isSameAs(failure);
        verify(redisService).deleteValues("ALERT:USER_TEST:1");
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
    @DisplayName("durable delivery는 네 채널에 provider-safe key를 전파하고 발송 이력을 남긴다")
    void deliversSeatOpeningWithProviderSafeIdempotencyKey() {
        // Given
        String canonicalKey = "d35e8a1f-3135-4f9b-92b3-635f0c9ea641";
        User user = User.builder()
                .id(1L)
                .email(EMAIL)
                .role(Role.USER)
                .emailEnabled(true)
                .webPushEnabled(true)
                .fcmEnabled(true)
                .discordEnabled(true)
                .discordId("discord-id")
                .build();
        addRecipient(user);
        addDevice(UserDevice.builder().userId(1L).type(DeviceType.FCM).token(DEVICE_TOKEN).build());
        addDevice(UserDevice.builder().userId(1L).type(DeviceType.WEB)
                .token("web-endpoint").p256dh("p256").auth("auth").build());
        SeatNotificationOutbox outbox = SeatNotificationOutbox.builder()
                .courseKey(COURSE_KEY)
                .courseName(COURSE_NAME)
                .professor(PROFESSOR)
                .previousSeats(0)
                .currentSeats(1)
                .build();
        for (NotificationChannel channel : NotificationChannel.values()) {
            given(notificationSender.supports(channel)).willReturn(true);
        }

        // When
        for (NotificationChannel channel : NotificationChannel.values()) {
            notificationService.deliverSeatOpening(1L, channel, outbox, canonicalKey);
        }

        // Then
        verify(notificationSender, times(4)).send(any(NotificationTarget.class), anyString(), anyString(),
                eq(NotificationDeliveryIdempotencyKey.providerKey(canonicalKey)));
        verify(notificationHistoryRepository, times(4)).save(any(NotificationHistory.class));
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
        NotificationDeviceType type = device.getType() == DeviceType.WEB
                ? NotificationDeviceType.WEB : NotificationDeviceType.FCM;
        devicesByUser.computeIfAbsent(device.getUserId(), ignored -> new java.util.ArrayList<>())
                .add(new NotificationDevice(device.getUserId(), type, device.getToken(),
                        device.getP256dh(), device.getAuth()));
    }
}
