package bhoon.sugang_helper.domain.admin.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "관리자 대시보드 통계 응답 DTO")
public class AdminDashboardResponse {
    @Schema(description = "전체 사용자 수", example = "150")
    private long totalUsers;

    @Schema(description = "전체 활성 구독 수", example = "450")
    private long totalActiveSubscriptions;

    @Schema(description = "오늘의 알림 발송 수", example = "120")
    private long todayNotificationCount;

    @Schema(description = "크롤링 상태", example = "RUNNING")
    private String crawlingStatus;

    @Schema(description = "마지막 크롤링 일시", example = "2024-01-01T12:00:00")
    private String lastCrawledAt;
}
