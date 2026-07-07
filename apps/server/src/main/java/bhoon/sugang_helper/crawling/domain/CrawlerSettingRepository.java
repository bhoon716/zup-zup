package bhoon.sugang_helper.crawling.domain;

import bhoon.sugang_helper.common.domain.RepositoryContract;
import java.util.Optional;

public interface CrawlerSettingRepository extends RepositoryContract<CrawlerSetting, Long> {

    Optional<CrawlerSetting> findTopByOrderByIdAsc();
}
