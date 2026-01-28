package bhoon.sugang_helper.domain.timetable.repository;

import bhoon.sugang_helper.domain.timetable.entity.CustomSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomScheduleRepository extends JpaRepository<CustomSchedule, Long> {
}
