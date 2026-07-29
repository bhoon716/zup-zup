package bhoon.sugang_helper.crawling.infra;

import bhoon.sugang_helper.crawling.domain.CrawlerRunFailure;
import bhoon.sugang_helper.crawling.domain.CrawlerRunFailureRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawlerRunFailureJpaRepository
        extends JpaRepository<CrawlerRunFailure, Long>, CrawlerRunFailureRepository {
}
