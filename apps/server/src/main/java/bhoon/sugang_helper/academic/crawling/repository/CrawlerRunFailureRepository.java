package bhoon.sugang_helper.academic.crawling.repository;

import bhoon.sugang_helper.academic.crawling.entity.CrawlerRunFailure;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawlerRunFailureRepository extends JpaRepository<CrawlerRunFailure, Long> {
}
