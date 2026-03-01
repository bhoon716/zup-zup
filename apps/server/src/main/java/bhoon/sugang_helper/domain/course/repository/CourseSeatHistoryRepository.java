package bhoon.sugang_helper.domain.course.repository;

import bhoon.sugang_helper.domain.course.entity.CourseSeatHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseSeatHistoryRepository extends JpaRepository<CourseSeatHistory, Long> {
    List<CourseSeatHistory> findByCourseKeyOrderByCreatedAtDesc(String courseKey);
}
