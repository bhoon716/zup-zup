package bhoon.sugang_helper.dashboard.dto.response;

import bhoon.sugang_helper.campus.api.AnnouncementPreview;
import bhoon.sugang_helper.campus.api.ScheduleSummary;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DashboardSnapshotResponse {

    private final DashboardUserResult user;

    private final List<DashboardNotificationResult> notifications;

    private final DashboardPrimaryTimetableResult primaryTimetable;

    private final List<ScheduleSummary> upcomingSchedules;

    private final List<AnnouncementPreview> announcements;

    public static DashboardSnapshotResponse of(DashboardUserResult user,
                                                List<DashboardNotificationResult> notifications,
                                                DashboardPrimaryTimetableResult primaryTimetable,
                                                List<ScheduleSummary> upcomingSchedules,
                                                List<AnnouncementPreview> announcements) {
        return new DashboardSnapshotResponse(user, notifications, primaryTimetable, upcomingSchedules, announcements);
    }
}
