package bhoon.sugang_helper.dashboard.dto.response;

import bhoon.sugang_helper.campus.announcement.dto.AnnouncementPreview;
import bhoon.sugang_helper.campus.schedule.dto.ScheduleSummary;
import bhoon.sugang_helper.dashboard.dto.DashboardNotificationResult;
import bhoon.sugang_helper.dashboard.dto.DashboardPrimaryTimetableResult;
import bhoon.sugang_helper.dashboard.dto.DashboardUserResult;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardSnapshotResponse {

    private DashboardUserResult user;

    private List<DashboardNotificationResult> notifications;

    private DashboardPrimaryTimetableResult primaryTimetable;

    private List<ScheduleSummary> upcomingSchedules;

    private List<AnnouncementPreview> announcements;
}
