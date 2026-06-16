package bhoon.sugang_helper.timetable.infra;

import bhoon.sugang_helper.timetable.domain.CustomSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomScheduleRepository extends JpaRepository<CustomSchedule, Long> {
}
