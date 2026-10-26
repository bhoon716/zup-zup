package bhoon.sugang_helper.academic.course.repository;

import bhoon.sugang_helper.academic.course.entity.CourseSeatHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseSeatHistoryRepository extends JpaRepository<CourseSeatHistory, Long> {
    Slice<CourseSeatHistory> findByCourseKeyOrderByCreatedAtDescIdDesc(String courseKey, Pageable pageable);
}
