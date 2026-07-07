package bhoon.sugang_helper.crawling.infra;

import bhoon.sugang_helper.crawling.domain.CrawlerSetting;
import bhoon.sugang_helper.crawling.domain.CrawlerSettingRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawlerSettingJpaRepository extends JpaRepository<CrawlerSetting, Long>, CrawlerSettingRepository {

    Optional<CrawlerSetting> findTopByOrderByIdAsc();
}
