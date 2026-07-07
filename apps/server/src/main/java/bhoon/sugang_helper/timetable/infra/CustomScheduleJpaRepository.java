package bhoon.sugang_helper.timetable.infra;

import bhoon.sugang_helper.timetable.domain.CustomSchedule;
import bhoon.sugang_helper.timetable.domain.CustomScheduleRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomScheduleJpaRepository extends JpaRepository<CustomSchedule, Long>, CustomScheduleRepository {
}
