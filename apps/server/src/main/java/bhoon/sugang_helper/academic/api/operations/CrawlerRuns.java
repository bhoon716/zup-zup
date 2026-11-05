package bhoon.sugang_helper.academic.api.operations;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CrawlerRuns {

    Optional<LocalDateTime> findLastSuccessAt();
}
