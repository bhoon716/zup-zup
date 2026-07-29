package bhoon.sugang_helper.crawling.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.crawling.domain.CrawlerFailureStage;
import bhoon.sugang_helper.crawling.domain.CrawlerRunFailure;
import bhoon.sugang_helper.crawling.domain.CrawlerRunFailureRepository;
import bhoon.sugang_helper.crawling.domain.CrawlerRunStatus;
import bhoon.sugang_helper.crawling.domain.CrawlerStatus;
import bhoon.sugang_helper.crawling.domain.CrawlerStatusRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class CrawlerRunStateServiceTest {

    @Mock
    private CrawlerStatusRepository crawlerStatusRepository;
    @Mock
    private CrawlerRunFailureRepository crawlerRunFailureRepository;

    @InjectMocks
    private CrawlerRunStateService service;

    @Test
    @DisplayName("성공 시 singleton crawler_status의 마지막 성공 시각을 갱신한다")
    void markSuccess_UpdatesSingletonStatus() {
        given(crawlerStatusRepository.findById(CrawlerStatus.SINGLETON_ID)).willReturn(Optional.empty());

        service.markSuccess();

        ArgumentCaptor<CrawlerStatus> captor = ArgumentCaptor.forClass(CrawlerStatus.class);
        verify(crawlerStatusRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(CrawlerStatus.SINGLETON_ID);
        assertThat(captor.getValue().getLatestStatus()).isEqualTo(CrawlerRunStatus.SUCCESS);
        assertThat(captor.getValue().getLastSuccessAt()).isNotNull();
    }

    @Test
    @DisplayName("실패 시 상태와 실행별 영구 실패 행에 비민감 요약을 함께 기록한다")
    void recordFailure_SavesStatusAndFailureRow() {
        given(crawlerStatusRepository.findById(CrawlerStatus.SINGLETON_ID)).willReturn(Optional.empty());
        CustomException exception = new CustomException(
                ErrorCode.CRAWLER_CONNECTION_ERROR, "외부 응답 원문 secret=do-not-store");

        service.recordFailure(CrawlerFailureStage.FETCH_PARSE, exception);

        ArgumentCaptor<CrawlerStatus> statusCaptor = ArgumentCaptor.forClass(CrawlerStatus.class);
        ArgumentCaptor<CrawlerRunFailure> failureCaptor = ArgumentCaptor.forClass(CrawlerRunFailure.class);
        verify(crawlerStatusRepository).save(statusCaptor.capture());
        verify(crawlerRunFailureRepository).save(failureCaptor.capture());
        assertThat(statusCaptor.getValue().getLatestStatus()).isEqualTo(CrawlerRunStatus.FAILURE);
        assertThat(failureCaptor.getValue().getFailureMessage())
                .contains(ErrorCode.CRAWLER_CONNECTION_ERROR.getCode())
                .doesNotContain("secret", "do-not-store");
    }

    @Test
    @DisplayName("예상하지 못한 예외 메시지 원문도 실패 DB 기록에 저장하지 않는다")
    void unexpectedFailure_DoesNotPersistRawMessage() {
        CrawlerFailureSummary summary = CrawlerFailureSummary.from(
                CrawlerFailureStage.PERSIST, new IllegalStateException("password=secret"));

        assertThat(summary.failureType()).isEqualTo("IllegalStateException");
        assertThat(summary.failureMessage()).isEqualTo("Unexpected crawler failure");
    }

    @Test
    @DisplayName("실패 기록은 rollback된 크롤러 트랜잭션과 분리된 새 트랜잭션에서 저장한다")
    void recordFailure_RequiresNewTransaction() throws NoSuchMethodException {
        Transactional transactional = CrawlerRunStateService.class
                .getMethod("recordFailure", CrawlerFailureStage.class, RuntimeException.class)
                .getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }
}
