package bhoon.sugang_helper.admin.dto.response;

import bhoon.sugang_helper.seatalert.api.NotificationHistoryItem;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminRecentLogResponse {

    private static final String CRAWLER_SOURCE = "CourseCrawler";

    private final LocalDateTime timestamp;

    private final String level;

    private final String message;

    private final String source;

    public static AdminRecentLogResponse crawlerHistoryMissing(LocalDateTime checkedAt) {
        return new AdminRecentLogResponse(
                checkedAt, "WARN", "크롤링 이력이 없어 상태를 확인할 수 없습니다.", CRAWLER_SOURCE);
    }

    public static AdminRecentLogResponse crawlerDelayed(LocalDateTime detectedAt) {
        return new AdminRecentLogResponse(detectedAt, "WARN", "크롤러 실행 지연이 감지되었습니다.", CRAWLER_SOURCE);
    }

    public static AdminRecentLogResponse crawlerHealthy(LocalDateTime lastCrawledAt) {
        return new AdminRecentLogResponse(lastCrawledAt, "INFO", "크롤러 주기가 정상 동작 중입니다.", CRAWLER_SOURCE);
    }

    public static AdminRecentLogResponse from(NotificationHistoryItem history) {
        String message = String.format("알림 발송 완료: %s (%s)", history.title(), history.channel().name());
        return new AdminRecentLogResponse(history.createdAt(), "INFO", message, "NotificationService");
    }
}
