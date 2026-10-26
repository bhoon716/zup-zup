package bhoon.sugang_helper.academic.crawling.service;

import bhoon.sugang_helper.academic.crawling.dto.response.CrawlerAdminSnapshot;

public interface CrawlerAdminSnapshotQuery {

    CrawlerAdminSnapshot findCurrentAdminSnapshot();
}
