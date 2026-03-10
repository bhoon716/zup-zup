package bhoon.sugang_helper.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.common.config.NotificationProperties;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
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
                // 테스트 기본값: 모든 알림 채널 활성화
                NotificationProperties props = new NotificationProperties(true, true, true, true);
                notificationService = new NotificationService(redisService, subscriptionRepository, userRepository,
                                userDeviceRepository, notificationHistoryRepository, List.of(notificationSender),
                                props);
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
        @DisplayName("구독자에게 멀티 채널(Email, FCM, WEB, Discord) 발송 검증")
        void dispatchToMultiChannels() {
                // Given
                SeatOpenedEvent event = new SeatOpenedEvent("12345-01", "Test Course", 0, 1);
                Subscription subscription = Subscription.builder().userId(1L).courseKey("12345-01").isActive(true)
                                .build();
                User user = User.builder()
                                .id(1L)
                                .name("Tester")
                                .email("test@example.com")
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

                given(redisService.hasKey(anyString())).willReturn(false);
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
                verify(notificationHistoryRepository, times(4)).save(any(NotificationHistory.class));
        }

        @Test
        @DisplayName("관리자 테스트 알림 발송 - 이메일")
        void sendTestNotification_Email() {
                // Given
                User user = User.builder()
                                .id(1L)
                                .email("test@example.com")
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
                // given
                User user = User.builder()
                                .id(1L)
                                .email("test@example.com")
                                .build();
                when(redisService.setValuesIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
                given(notificationSender.supports(NotificationChannel.EMAIL)).willReturn(true);

                // when
                notificationService.sendUserTestNotification(user, List.of(NotificationChannel.EMAIL));

                // then
                verify(notificationSender, times(1)).send(any(NotificationTarget.class), anyString(), anyString());
                verify(notificationHistoryRepository, never()).save(any(NotificationHistory.class));
        }

        @Test
        @DisplayName("사용자 테스트 알림 발송 - 쿨타임 중이면 예외 발생")
        void sendUserTestNotification_Cooldown() {
                // given
                User user = User.builder()
                                .id(1L)
                                .email("test@example.com")
                                .build();
                when(redisService.setValuesIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

                // when & then
                assertThatThrownBy(() -> notificationService.sendUserTestNotification(user,
                                List.of(NotificationChannel.EMAIL)))
                                .isInstanceOf(CustomException.class)
                                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOO_MANY_REQUESTS);
        }

        @Test
        @DisplayName("관리자 테스트 알림 발송 - 푸시 (FCM/WEB)")
        void sendTestNotification_Push() {
                // Given
                User user = User.builder().id(1L).email("test@example.com").build();
                UserDevice device = UserDevice.builder().userId(1L).type(DeviceType.FCM).token("token").build();

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
                User user = User.builder().id(1L).email("test@example.com").build();

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
                                .email("test@example.com")
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
                                .email("test@example.com")
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
