package bhoon.sugang_helper.academic.crawling.infra;

import bhoon.sugang_helper.academic.crawling.domain.CrawlerRunFailure;
import bhoon.sugang_helper.academic.crawling.domain.CrawlerRunFailureRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawlerRunFailureJpaRepository
        extends JpaRepository<CrawlerRunFailure, Long>, CrawlerRunFailureRepository {
}
