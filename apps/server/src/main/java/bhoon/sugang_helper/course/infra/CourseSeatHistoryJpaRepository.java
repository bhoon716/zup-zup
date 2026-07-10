package bhoon.sugang_helper.course.infra;

import bhoon.sugang_helper.course.domain.CourseSeatHistory;
import bhoon.sugang_helper.course.domain.CourseSeatHistoryRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface CourseSeatHistoryJpaRepository extends JpaRepository<CourseSeatHistory, Long>,
        CourseSeatHistoryRepository {
    Slice<CourseSeatHistory> findByCourseKeyOrderByCreatedAtDescIdDesc(String courseKey, Pageable pageable);
}
