package bhoon.sugang_helper.crawling.application;

import bhoon.sugang_helper.crawling.domain.CrawlerFailureStage;
import bhoon.sugang_helper.crawling.domain.CrawlerRunFailure;
import bhoon.sugang_helper.crawling.domain.CrawlerRunFailureRepository;
import bhoon.sugang_helper.crawling.domain.CrawlerStatus;
import bhoon.sugang_helper.crawling.domain.CrawlerStatusRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CrawlerRunStateService {

    private final CrawlerStatusRepository crawlerStatusRepository;
    private final CrawlerRunFailureRepository crawlerRunFailureRepository;

    @Transactional
    public void markSuccess() {
        LocalDateTime occurredAt = LocalDateTime.now();
        CrawlerStatus status = crawlerStatusRepository.findById(CrawlerStatus.SINGLETON_ID)
                .orElseGet(CrawlerStatus::initial);
        status.markSuccess(occurredAt);
        crawlerStatusRepository.save(status);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(CrawlerFailureStage stage, RuntimeException exception) {
        LocalDateTime occurredAt = LocalDateTime.now();
        CrawlerFailureSummary summary = CrawlerFailureSummary.from(stage, exception);
        CrawlerStatus status = crawlerStatusRepository.findById(CrawlerStatus.SINGLETON_ID)
                .orElseGet(CrawlerStatus::initial);
        status.markFailure(occurredAt, summary.stage(), summary.failureType(), summary.failureMessage());
        crawlerStatusRepository.save(status);
        crawlerRunFailureRepository.save(new CrawlerRunFailure(
                occurredAt, summary.stage(), summary.failureType(), summary.failureMessage()));
    }

    @Transactional(readOnly = true)
    public Optional<LocalDateTime> findLastSuccessAt() {
        return crawlerStatusRepository.findById(CrawlerStatus.SINGLETON_ID)
                .map(CrawlerStatus::getLastSuccessAt);
    }
}
