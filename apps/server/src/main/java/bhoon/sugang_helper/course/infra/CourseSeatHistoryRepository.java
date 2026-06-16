package bhoon.sugang_helper.course.infra;

import bhoon.sugang_helper.course.domain.CourseSeatHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseSeatHistoryRepository extends JpaRepository<CourseSeatHistory, Long> {
    List<CourseSeatHistory> findByCourseKeyOrderByCreatedAtDesc(String courseKey);
}
