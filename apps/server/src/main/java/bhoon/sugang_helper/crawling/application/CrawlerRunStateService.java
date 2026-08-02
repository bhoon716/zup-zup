package bhoon.sugang_helper.crawling.application;

import bhoon.sugang_helper.common.alert.SlackAlertCategory;
import bhoon.sugang_helper.common.alert.SlackAlertService;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.security.util.SensitiveDataRedactor;
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
    private final SlackAlertService slackAlertService;

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
        slackAlertService.alert(alertCategory(stage, exception), SensitiveDataRedactor.failureCode(exception), exception);
    }

    private SlackAlertCategory alertCategory(CrawlerFailureStage stage, RuntimeException exception) {
        if (stage == CrawlerFailureStage.PERSIST) {
            return SlackAlertCategory.DB_PERSIST;
        }
        if (exception instanceof CustomException customException
                && customException.getErrorCode() == ErrorCode.CRAWLER_PARSING_ERROR) {
            return SlackAlertCategory.CRAWLER_PARSE;
        }
        return SlackAlertCategory.CRAWLER_FETCH;
    }

    @Transactional(readOnly = true)
    public Optional<LocalDateTime> findLastSuccessAt() {
        return crawlerStatusRepository.findById(CrawlerStatus.SINGLETON_ID)
                .map(CrawlerStatus::getLastSuccessAt);
    }
}
