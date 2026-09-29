package bhoon.sugang_helper.campus.schedule.contract;

import java.util.List;

public interface UpcomingScheduleQuery {

    List<ScheduleSummary> findUpcomingSchedules();
}
