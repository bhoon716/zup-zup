package bhoon.sugang_helper.domain.dashboard.service;

import bhoon.sugang_helper.domain.announcement.repository.AnnouncementRepository;
import bhoon.sugang_helper.domain.announcement.response.AnnouncementListResponse;
import bhoon.sugang_helper.domain.dashboard.response.DashboardSnapshotResponse;
import bhoon.sugang_helper.domain.notification.repository.NotificationHistoryRepository;
import bhoon.sugang_helper.domain.notification.response.NotificationHistoryResponse;
import bhoon.sugang_helper.domain.schedule.response.ScheduleResponse;
import bhoon.sugang_helper.domain.schedule.service.ScheduleService;
import bhoon.sugang_helper.domain.timetable.response.TimetableDetailResponse;
import bhoon.sugang_helper.domain.timetable.service.TimetableService;
import bhoon.sugang_helper.domain.user.response.UserResponse;
import bhoon.sugang_helper.domain.user.service.UserService;
import java.util.List;
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
        UserResponse user = userService.getMyProfile();
        Long userId = user.getId();

        List<NotificationHistoryResponse> notifications = notificationHistoryRepository
                .findTop3ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationHistoryResponse::from)
                .toList();

        TimetableDetailResponse primaryTimetable = timetableService.getPrimaryTimetable();
        List<ScheduleResponse> upcomingSchedules = scheduleService.getUpcomingSchedules();
        List<AnnouncementListResponse> announcements = announcementRepository
                .findByPublishedTrueOrderByPinnedDescCreatedAtDesc(PageRequest.of(0, DASHBOARD_ANNOUNCEMENT_LIMIT))
                .stream()
                .map(AnnouncementListResponse::from)
                .toList();

        return DashboardSnapshotResponse.builder()
                .user(user)
                .notifications(notifications)
                .primaryTimetable(primaryTimetable)
                .upcomingSchedules(upcomingSchedules)
                .announcements(announcements)
                .build();
    }
}
