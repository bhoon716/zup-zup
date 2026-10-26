package bhoon.sugang_helper.planning.timetable.repository;

import bhoon.sugang_helper.planning.timetable.entity.CustomSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomScheduleRepository extends JpaRepository<CustomSchedule, Long> {
}
