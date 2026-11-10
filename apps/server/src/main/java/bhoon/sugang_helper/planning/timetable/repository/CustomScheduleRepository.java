package bhoon.sugang_helper.planning.timetable.repository;

import bhoon.sugang_helper.planning.timetable.entity.CustomSchedule;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomScheduleRepository extends JpaRepository<CustomSchedule, Long> {
    @EntityGraph(attributePaths = "times")
    List<CustomSchedule> findWithTimesByTimetableId(Long timetableId);
}
