package bhoon.sugang_helper.crawling.infra;

import bhoon.sugang_helper.crawling.domain.CrawlerStatus;
import bhoon.sugang_helper.crawling.domain.CrawlerStatusRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawlerStatusJpaRepository extends JpaRepository<CrawlerStatus, Long>, CrawlerStatusRepository {
}
