package bhoon.sugang_helper.course.domain;

import bhoon.sugang_helper.common.domain.RepositoryContract;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface CourseSeatHistoryRepository extends RepositoryContract<CourseSeatHistory, Long> {
    Slice<CourseSeatHistory> findByCourseKeyOrderByCreatedAtDescIdDesc(String courseKey, Pageable pageable);
}
