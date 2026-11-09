package bhoon.sugang_helper.academic.crawling.service;

import bhoon.sugang_helper.academic.api.operations.CrawlerRuns;
import bhoon.sugang_helper.academic.crawling.dto.diagnostic.CrawlerFailureSummary;
import bhoon.sugang_helper.academic.crawling.entity.CrawlerStatus;
import bhoon.sugang_helper.academic.crawling.enums.CrawlerFailureStage;
import bhoon.sugang_helper.academic.crawling.repository.CrawlerStatusRepository;
import bhoon.sugang_helper.common.api.alerts.SlackAlertCategory;
import bhoon.sugang_helper.common.api.alerts.OperationalAlerts;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.api.security.SensitiveDataRedactor;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerRunStateService implements CrawlerRuns {

    private final CrawlerStatusRepository crawlerStatusRepository;
    private final CrawlerFailurePersistenceService crawlerFailurePersistenceService;
    private final OperationalAlerts slackAlertService;

    @Transactional
    public void markSuccess() {
        LocalDateTime occurredAt = LocalDateTime.now();
        CrawlerStatus status = crawlerStatusRepository.findById(CrawlerStatus.SINGLETON_ID)
                .orElseGet(CrawlerStatus::initial);
        status.markSuccess(occurredAt);
        crawlerStatusRepository.save(status);
    }

    public void recordFailure(CrawlerFailureStage stage, RuntimeException exception) {
        LocalDateTime occurredAt = LocalDateTime.now();
        CrawlerFailureSummary summary = CrawlerFailureSummary.from(stage, exception);
        try {
            crawlerFailurePersistenceService.recordFailure(occurredAt, summary);
        } catch (RuntimeException recordingException) {
            logFailureRecordingFailed(summary, exception, recordingException);
        }
        try {
            alertFailure(summary, exception);
        } catch (RuntimeException alertException) {
            logFailureAlertFailed(summary, exception, alertException);
        }
    }

    private void logFailureRecordingFailed(CrawlerFailureSummary summary, RuntimeException originalException,
                                           RuntimeException recordingException) {
        log.atError().addKeyValue("event", "crawler_failure_record_failed")
                .addKeyValue("stage", summary.stage().name())
                .addKeyValue("originalType", SensitiveDataRedactor.exceptionType(originalException))
                .addKeyValue("recordingType", SensitiveDataRedactor.exceptionType(recordingException))
                .log("[Crawler] 수집 실패 이력 저장 실패. stage={}, originalType={}, recordingType={}",
                        summary.stage(), SensitiveDataRedactor.exceptionType(originalException),
                        SensitiveDataRedactor.exceptionType(recordingException));
    }

    private void logFailureAlertFailed(CrawlerFailureSummary summary, RuntimeException originalException,
                                       RuntimeException alertException) {
        log.atError().addKeyValue("event", "crawler_failure_alert_failed")
                .addKeyValue("stage", summary.stage().name())
                .addKeyValue("originalType", SensitiveDataRedactor.exceptionType(originalException))
                .addKeyValue("alertType", SensitiveDataRedactor.exceptionType(alertException))
                .log("[Crawler] 수집 실패 알림 요청 실패. stage={}, originalType={}, alertType={}",
                        summary.stage(), SensitiveDataRedactor.exceptionType(originalException),
                        SensitiveDataRedactor.exceptionType(alertException));
    }

    private void alertFailure(CrawlerFailureSummary summary, RuntimeException exception) {
        SlackAlertCategory category = alertCategory(summary.stage(), exception);
        String diagnostic = summary.diagnostic();
        if (diagnostic.isBlank()) {
            slackAlertService.alert(category, SensitiveDataRedactor.failureCode(exception), exception);
            return;
        }
        slackAlertService.alert(category, SensitiveDataRedactor.failureCode(exception), exception, diagnostic);
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
