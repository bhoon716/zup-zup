package bhoon.sugang_helper.dashboard.service;

import bhoon.sugang_helper.campus.api.AnnouncementPreview;
import bhoon.sugang_helper.campus.api.Announcements;
import bhoon.sugang_helper.campus.api.ScheduleSummary;
import bhoon.sugang_helper.campus.api.AcademicSchedules;
import bhoon.sugang_helper.dashboard.dto.response.DashboardNotificationResult;
import bhoon.sugang_helper.dashboard.dto.response.DashboardPrimaryTimetableResult;
import bhoon.sugang_helper.dashboard.dto.response.DashboardUserResult;
import bhoon.sugang_helper.dashboard.dto.response.DashboardSnapshotResponse;
import bhoon.sugang_helper.identity.api.users.UserDashboardProfile;
import bhoon.sugang_helper.identity.api.users.UserDashboardProfiles;
import bhoon.sugang_helper.planning.api.Timetables;
import bhoon.sugang_helper.seatalert.api.NotificationHistoryQueries;
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

    private final UserDashboardProfiles userDashboardQuery;
    private final NotificationHistoryQueries notificationHistoryQuery;
    private final Timetables primaryTimetableQuery;
    private final AcademicSchedules upcomingScheduleQuery;
    private final Announcements dashboardAnnouncementQuery;

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
