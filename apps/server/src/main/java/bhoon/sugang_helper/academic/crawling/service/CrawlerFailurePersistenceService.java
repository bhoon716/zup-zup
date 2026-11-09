package bhoon.sugang_helper.academic.crawling.service;

import bhoon.sugang_helper.academic.crawling.dto.diagnostic.CrawlerFailureSummary;
import bhoon.sugang_helper.academic.crawling.entity.CrawlerRunFailure;
import bhoon.sugang_helper.academic.crawling.entity.CrawlerStatus;
import bhoon.sugang_helper.academic.crawling.repository.CrawlerRunFailureRepository;
import bhoon.sugang_helper.academic.crawling.repository.CrawlerStatusRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CrawlerFailurePersistenceService {

    private final CrawlerStatusRepository crawlerStatusRepository;
    private final CrawlerRunFailureRepository crawlerRunFailureRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(LocalDateTime occurredAt, CrawlerFailureSummary summary) {
        CrawlerStatus status = crawlerStatusRepository.findById(CrawlerStatus.SINGLETON_ID)
                .orElseGet(CrawlerStatus::initial);
        status.markFailure(occurredAt, summary.stage(), summary.failureType(), summary.failureMessage());
        crawlerStatusRepository.save(status);
        crawlerRunFailureRepository.save(new CrawlerRunFailure(
                occurredAt, summary.stage(), summary.failureType(), summary.failureMessage()));
    }
}
