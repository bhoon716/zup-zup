package bhoon.sugang_helper.domain.admin.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "관리자 대시보드 개요 응답 DTO")
public class AdminOverviewResponse {

    @Schema(description = "전체 사용자 수", example = "5402")
    private long totalUsers;

    @Schema(description = "활성 구독 수", example = "890")
    private long totalActiveSubscriptions;

    @Schema(description = "오늘 발송된 알림 수", example = "145")
    private long todayNotificationCount;

    @Schema(description = "크롤러 상태", example = "RUNNING")
    private String crawlingStatus;

    @Schema(description = "마지막 크롤링 시각", example = "2026-02-20T14:31:12")
    private LocalDateTime lastCrawledAt;

    @Schema(description = "JBNU 서버 평균 지연시간(ms). 미수집 시 null", example = "45")
    private Long jbnuLatencyMs;

    @Schema(description = "서버 현재 시각", example = "2026-02-20T14:32:05")
    private LocalDateTime serverTime;

    @Schema(description = "최근 24시간 시간대별 알림 트래픽")
    private List<AdminHourlyTrafficResponse> notificationTraffic;

    @Schema(description = "최근 시스템 로그")
    private List<AdminRecentLogResponse> recentLogs;
}
