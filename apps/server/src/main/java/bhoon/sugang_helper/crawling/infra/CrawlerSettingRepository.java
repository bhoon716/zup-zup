package bhoon.sugang_helper.crawling.infra;

import bhoon.sugang_helper.crawling.domain.CrawlerSetting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawlerSettingRepository extends JpaRepository<CrawlerSetting, Long> {

    Optional<CrawlerSetting> findTopByOrderByIdAsc();
}
