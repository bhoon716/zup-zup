package bhoon.sugang_helper.academic.crawling.service;

import bhoon.sugang_helper.academic.crawling.dto.CrawlerFailureSummary;
import bhoon.sugang_helper.academic.crawling.entity.CrawlerRunFailure;
import bhoon.sugang_helper.academic.crawling.entity.CrawlerStatus;
import bhoon.sugang_helper.academic.crawling.enums.CrawlerFailureStage;
import bhoon.sugang_helper.academic.crawling.repository.CrawlerRunFailureRepository;
import bhoon.sugang_helper.academic.crawling.repository.CrawlerStatusRepository;
import bhoon.sugang_helper.common.alert.SlackAlertCategory;
import bhoon.sugang_helper.common.alert.SlackAlertService;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.security.util.SensitiveDataRedactor;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CrawlerRunStateService implements CrawlerStatusQuery {

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
        SlackAlertCategory category = alertCategory(summary.stage(), exception);
        String diagnostic = summary.diagnostic();
        if (diagnostic.isBlank()) {
            slackAlertService.alert(category, SensitiveDataRedactor.failureCode(exception), exception);
        } else {
            slackAlertService.alert(category, SensitiveDataRedactor.failureCode(exception), exception, diagnostic);
        }
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

    @Override
    @Transactional(readOnly = true)
    public Optional<LocalDateTime> findLastSuccessAt() {
        return crawlerStatusRepository.findById(CrawlerStatus.SINGLETON_ID)
                .map(CrawlerStatus::getLastSuccessAt);
    }
}
