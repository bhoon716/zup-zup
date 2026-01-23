package bhoon.sugang_helper.domain.notification.service;

import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.domain.course.event.SeatOpenedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final RedisService redisService;
    private static final String NOTIFICATION_KEY_PREFIX = "ALERT:";
    private static final Duration DEDUP_TTL = Duration.ofMinutes(10);

    @Async
    @EventListener
    public void handleSeatOpenedEvent(SeatOpenedEvent event) {
        String courseKey = event.courseKey();
        String redisKey = NOTIFICATION_KEY_PREFIX + courseKey;

        if (redisService.hasKey(redisKey)) {
            log.info("[Dedup] Notification already sent for course: {}. Skipping.", courseKey);
            return;
        }

        // Logic to send notification (Phase 4/5 placeholder)
        sendNotification(event);

        // Set Dedup Key
        redisService.setValues(redisKey, "SENT", DEDUP_TTL);
        log.info("[Notification] Alert sent for course: {} (Redis Key set with TTL 30m)", courseKey);
    }

    private void sendNotification(SeatOpenedEvent event) {
        // Placeholder for actual notification sending (FCM, Email, etc.)
        log.info("Sending notification for course: {} - {} (Available: {})",
                event.courseKey(), event.courseName(), event.currentSeats());
    }
}
