package bhoon.sugang_helper.schedule.domain;

import bhoon.sugang_helper.common.domain.RepositoryContract;
import java.time.LocalDate;
import java.util.List;

public interface ScheduleRepository extends RepositoryContract<Schedule, Long> {
    List<Schedule> findAllByOrderByStartDateDescStartTimeDesc();

    List<Schedule> findUpcomingSchedules(LocalDate today);
}
