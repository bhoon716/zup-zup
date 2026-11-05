package bhoon.sugang_helper.academic.api.operations;

import java.time.LocalDateTime;
import java.util.Optional;

/** 다른 모듈이 사용하는 CrawlerRuns 계약. */
public interface CrawlerRuns {

    Optional<LocalDateTime> findLastSuccessAt();
}
