package bhoon.sugang_helper.campus.schedule.service;

import bhoon.sugang_helper.campus.schedule.dto.response.ScheduleSummary;
import java.util.List;

public interface UpcomingScheduleQuery {

    List<ScheduleSummary> findUpcomingSchedules();
}
