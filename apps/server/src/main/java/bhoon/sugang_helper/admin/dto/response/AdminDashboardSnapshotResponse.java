package bhoon.sugang_helper.admin.dto.response;

import bhoon.sugang_helper.academic.api.operations.CrawlerAdminSnapshot;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminDashboardSnapshotResponse {

    private final AdminOverviewResponse overview;

    private final CrawlerAdminSnapshot crawlTarget;

    public static AdminDashboardSnapshotResponse of(AdminOverviewResponse overview, CrawlerAdminSnapshot crawlTarget) {
        return new AdminDashboardSnapshotResponse(overview, crawlTarget);
    }
}
