package bhoon.sugang_helper.academic.crawling.service;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CrawlerStatusQuery {

    Optional<LocalDateTime> findLastSuccessAt();
}
