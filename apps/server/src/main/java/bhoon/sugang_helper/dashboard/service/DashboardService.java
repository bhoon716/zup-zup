package bhoon.sugang_helper.dashboard.service;

import bhoon.sugang_helper.campus.announcement.dto.response.AnnouncementPreview;
import bhoon.sugang_helper.campus.announcement.service.AnnouncementService;
import bhoon.sugang_helper.campus.schedule.dto.response.ScheduleSummary;
import bhoon.sugang_helper.campus.schedule.service.ScheduleService;
import bhoon.sugang_helper.dashboard.dto.response.DashboardNotificationResult;
import bhoon.sugang_helper.dashboard.dto.response.DashboardPrimaryTimetableResult;
import bhoon.sugang_helper.dashboard.dto.response.DashboardUserResult;
import bhoon.sugang_helper.dashboard.dto.response.DashboardSnapshotResponse;
import bhoon.sugang_helper.identity.user.dto.result.UserDashboardProfile;
import bhoon.sugang_helper.identity.user.service.UserDashboardQueryService;
import bhoon.sugang_helper.planning.timetable.service.TimetableService;
import bhoon.sugang_helper.seatalert.notification.service.NotificationHistoryQueryService;
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

    private final UserDashboardQueryService userDashboardQuery;
    private final NotificationHistoryQueryService notificationHistoryQuery;
    private final TimetableService primaryTimetableQuery;
    private final ScheduleService upcomingScheduleQuery;
    private final AnnouncementService dashboardAnnouncementQuery;

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
