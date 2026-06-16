package bhoon.sugang_helper.course.infra;

import bhoon.sugang_helper.course.domain.CourseSeatHistory;
import bhoon.sugang_helper.course.domain.CourseSeatHistoryRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseSeatHistoryJpaRepository extends JpaRepository<CourseSeatHistory, Long>, CourseSeatHistoryRepository {
    List<CourseSeatHistory> findByCourseKeyOrderByCreatedAtDesc(String courseKey);
}
