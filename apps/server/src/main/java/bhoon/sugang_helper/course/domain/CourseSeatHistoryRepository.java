package bhoon.sugang_helper.course.domain;

import bhoon.sugang_helper.common.domain.RepositoryContract;
import java.util.List;

public interface CourseSeatHistoryRepository extends RepositoryContract<CourseSeatHistory, Long> {
    List<CourseSeatHistory> findByCourseKeyOrderByCreatedAtDesc(String courseKey);
}
