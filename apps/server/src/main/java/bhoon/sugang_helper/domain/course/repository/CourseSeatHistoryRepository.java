package bhoon.sugang_helper.domain.course.repository;

import bhoon.sugang_helper.domain.course.entity.CourseSeatHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseSeatHistoryRepository extends JpaRepository<CourseSeatHistory, Long> {
    List<CourseSeatHistory> findByCourseKeyOrderByCreatedAtDesc(String courseKey);
}
