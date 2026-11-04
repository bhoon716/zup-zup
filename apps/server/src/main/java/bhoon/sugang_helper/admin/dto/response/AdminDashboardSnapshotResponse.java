package bhoon.sugang_helper.admin.dto.response;

import bhoon.sugang_helper.academic.crawling.dto.response.CrawlerAdminSnapshot;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminDashboardSnapshotResponse {

    private AdminOverviewResponse overview;

    private CrawlerAdminSnapshot crawlTarget;
}
