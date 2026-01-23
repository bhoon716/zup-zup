package bhoon.sugang_helper.domain.notification.service;

import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.domain.course.event.SeatOpenedEvent;
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

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("Send notification if dedup key does not exist")
    void sendNotificationIfKeyNotExists() {
        // Given
        SeatOpenedEvent event = new SeatOpenedEvent("12345-01", "Test Course", 0, 1);
        String redisKey = "ALERT:12345-01";

        given(redisService.hasKey(redisKey)).willReturn(false);

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
    }
}
