package bhoon.sugang_helper.domain.notification.service;

import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.domain.course.event.SeatOpenedEvent;
import bhoon.sugang_helper.domain.notification.sender.NotificationChannel;
import bhoon.sugang_helper.domain.notification.sender.NotificationSender;
import bhoon.sugang_helper.domain.subscription.entity.Subscription;
import bhoon.sugang_helper.domain.subscription.repository.SubscriptionRepository;
import bhoon.sugang_helper.domain.notification.repository.NotificationHistoryRepository;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.entity.Role;
import bhoon.sugang_helper.domain.user.repository.UserDeviceRepository;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

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
    @DisplayName("Send notification if dedup key does not exist")
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
    @DisplayName("Skip notification if dedup key exists")
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
    @DisplayName("Dispatch email to subscribers")
    void dispatchEmailToSubscribers() {
        // Given
        SeatOpenedEvent event = new SeatOpenedEvent("12345-01", "Test Course", 0, 1);
        String redisKey = "ALERT:12345-01";
        Subscription subscription = Subscription.builder()
                .userId(1L)
                .courseKey("12345-01")
                .isActive(true)
                .build();
        User user = User.builder()
                .name("Tester")
                .email("test@example.com")
                .role(Role.USER)
                .build();

        // Inject sender list manually because @InjectMocks doesn't handle List well if
        // notSpy
        notificationService = new NotificationService(redisService, subscriptionRepository, userRepository,
                userDeviceRepository, notificationHistoryRepository, List.of(notificationSender));

        given(redisService.hasKey(redisKey)).willReturn(false);
        given(subscriptionRepository.findByCourseKeyAndIsActiveTrue("12345-01")).willReturn(List.of(subscription));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(notificationSender.supports(NotificationChannel.EMAIL)).willReturn(true);

        // When
        notificationService.handleSeatOpenedEvent(event);

        // Then
        verify(notificationSender, times(1)).send(eq("test@example.com"), anyString(), anyString());
    }
}
