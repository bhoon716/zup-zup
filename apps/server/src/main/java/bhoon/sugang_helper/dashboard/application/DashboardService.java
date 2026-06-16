package bhoon.sugang_helper.dashboard.application;

import bhoon.sugang_helper.announcement.infra.AnnouncementRepository;
import bhoon.sugang_helper.announcement.presentation.AnnouncementListResponse;
import bhoon.sugang_helper.dashboard.presentation.DashboardSnapshotResponse;
import bhoon.sugang_helper.notification.infra.NotificationHistoryRepository;
import bhoon.sugang_helper.notification.presentation.NotificationHistoryResponse;
import bhoon.sugang_helper.schedule.presentation.ScheduleResponse;
import bhoon.sugang_helper.schedule.application.ScheduleService;
import bhoon.sugang_helper.timetable.presentation.TimetableDetailResponse;
import bhoon.sugang_helper.timetable.application.TimetableService;
import bhoon.sugang_helper.user.domain.User;
import bhoon.sugang_helper.user.presentation.UserResponse;
import bhoon.sugang_helper.user.application.UserService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final int DASHBOARD_ANNOUNCEMENT_LIMIT = 4;

    private final UserService userService;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final TimetableService timetableService;
    private final ScheduleService scheduleService;
    private final AnnouncementRepository announcementRepository;

    public DashboardSnapshotResponse getDashboardSnapshot() {
        List<ScheduleResponse> upcomingSchedules = scheduleService.getUpcomingSchedules();
        List<AnnouncementListResponse> announcements = announcementRepository
                .findByPublishedTrueOrderByPinnedDescCreatedAtDesc(PageRequest.of(0, DASHBOARD_ANNOUNCEMENT_LIMIT))
                .stream()
                .map(AnnouncementListResponse::from)
                .toList();

        Optional<User> currentUser = userService.getCurrentUserOrNull();
        if (currentUser.isEmpty()) {
            return DashboardSnapshotResponse.builder()
                    .user(null)
                    .notifications(List.of())
                    .primaryTimetable(null)
                    .upcomingSchedules(upcomingSchedules)
                    .announcements(announcements)
                    .build();
        }

        User user = currentUser.get();
        UserResponse userResponse = UserResponse.from(user);
        Long userId = user.getId();

        List<NotificationHistoryResponse> notifications = notificationHistoryRepository
                .findTop3ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationHistoryResponse::from)
                .toList();

        TimetableDetailResponse primaryTimetable = timetableService.getPrimaryTimetable();

        return DashboardSnapshotResponse.builder()
                .user(userResponse)
                .notifications(notifications)
                .primaryTimetable(primaryTimetable)
                .upcomingSchedules(upcomingSchedules)
                .announcements(announcements)
                .build();
    }
}
