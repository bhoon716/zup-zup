package bhoon.sugang_helper.domain.admin.response;

import bhoon.sugang_helper.domain.course.response.AdminCrawlTargetResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "관리자 대시보드 스냅샷 응답 DTO")
public class AdminDashboardSnapshotResponse {

    @Schema(description = "관리자 대시보드 개요")
    private AdminOverviewResponse overview;

    @Schema(description = "기본 크롤링 타겟")
    private AdminCrawlTargetResponse crawlTarget;
}
