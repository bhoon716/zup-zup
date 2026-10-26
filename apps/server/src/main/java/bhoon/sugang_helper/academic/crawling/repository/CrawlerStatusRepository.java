package bhoon.sugang_helper.academic.crawling.repository;

import bhoon.sugang_helper.academic.crawling.entity.CrawlerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawlerStatusRepository extends JpaRepository<CrawlerStatus, Long> {
}
