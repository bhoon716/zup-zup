package bhoon.sugang_helper.dashboard.service;

import bhoon.sugang_helper.campus.announcement.dto.response.AnnouncementPreview;
import bhoon.sugang_helper.campus.announcement.service.DashboardAnnouncementQuery;
import bhoon.sugang_helper.campus.schedule.dto.response.ScheduleSummary;
import bhoon.sugang_helper.campus.schedule.service.UpcomingScheduleQuery;
import bhoon.sugang_helper.dashboard.dto.response.DashboardNotificationResult;
import bhoon.sugang_helper.dashboard.dto.response.DashboardPrimaryTimetableResult;
import bhoon.sugang_helper.dashboard.dto.response.DashboardUserResult;
import bhoon.sugang_helper.dashboard.dto.response.DashboardSnapshotResponse;
import bhoon.sugang_helper.identity.user.dto.result.UserDashboardProfile;
import bhoon.sugang_helper.identity.user.service.UserDashboardQuery;
import bhoon.sugang_helper.planning.timetable.service.PrimaryTimetableQuery;
import bhoon.sugang_helper.seatalert.notification.service.NotificationHistoryQuery;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final int DASHBOARD_ANNOUNCEMENT_LIMIT = 4;

    private final UserDashboardQuery userDashboardQuery;
    private final NotificationHistoryQuery notificationHistoryQuery;
    private final PrimaryTimetableQuery primaryTimetableQuery;
    private final UpcomingScheduleQuery upcomingScheduleQuery;
    private final DashboardAnnouncementQuery dashboardAnnouncementQuery;

    public DashboardSnapshotResponse getDashboardSnapshot() {
        List<ScheduleSummary> upcomingSchedules = upcomingScheduleQuery.findUpcomingSchedules();
        List<AnnouncementPreview> announcements = dashboardAnnouncementQuery
                .findDashboardAnnouncements(DASHBOARD_ANNOUNCEMENT_LIMIT);

        Optional<UserDashboardProfile> currentUser = userDashboardQuery.findCurrentUser();
        if (currentUser.isEmpty()) {
            return DashboardSnapshotResponse.builder()
                    .user(null)
                    .notifications(List.of())
                    .primaryTimetable(null)
                    .upcomingSchedules(upcomingSchedules)
                    .announcements(announcements)
                    .build();
        }

        UserDashboardProfile user = currentUser.get();
        DashboardUserResult userResponse = DashboardUserResult.from(user);
        Long userId = user.id();

        List<DashboardNotificationResult> notifications = notificationHistoryQuery
                .findLatestForUser(userId)
                .stream()
                .map(DashboardNotificationResult::from)
                .toList();

        var primaryTimetable = primaryTimetableQuery.findPrimaryForCurrentUser().orElse(null);

        return DashboardSnapshotResponse.builder()
                .user(userResponse)
                .notifications(notifications)
                .primaryTimetable(
                        primaryTimetable == null ? null : DashboardPrimaryTimetableResult.from(primaryTimetable))
                .upcomingSchedules(upcomingSchedules)
                .announcements(announcements)
                .build();
    }
}
