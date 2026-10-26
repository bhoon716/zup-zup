package bhoon.sugang_helper.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.campus.announcement.dto.AnnouncementPreview;
import bhoon.sugang_helper.campus.announcement.service.DashboardAnnouncementQuery;
import bhoon.sugang_helper.campus.schedule.dto.ScheduleSummary;
import bhoon.sugang_helper.campus.schedule.service.UpcomingScheduleQuery;
import bhoon.sugang_helper.dashboard.dto.response.DashboardSnapshotResponse;
import bhoon.sugang_helper.identity.user.dto.UserDashboardProfile;
import bhoon.sugang_helper.identity.user.service.UserDashboardQuery;
import bhoon.sugang_helper.planning.timetable.dto.PrimaryTimetableSnapshot;
import bhoon.sugang_helper.planning.timetable.service.PrimaryTimetableQuery;
import bhoon.sugang_helper.seatalert.enums.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.dto.NotificationHistoryItem;
import bhoon.sugang_helper.seatalert.notification.service.NotificationHistoryQuery;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private UserDashboardQuery userDashboardQuery;
    @Mock
    private NotificationHistoryQuery notificationHistoryQuery;
    @Mock
    private PrimaryTimetableQuery primaryTimetableQuery;
    @Mock
    private UpcomingScheduleQuery upcomingScheduleQuery;
    @Mock
    private DashboardAnnouncementQuery dashboardAnnouncementQuery;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("대시보드 스냅샷을 한 번에 조회한다")
    void getDashboardSnapshot() {
        UserDashboardProfile user = new UserDashboardProfile(
                7L, "user@example.com", "홍길동", "USER", "user@example.com",
                true, true, true, true, null, false);
        NotificationHistoryItem notification = new NotificationHistoryItem(
                1L, 7L, "TEST-101", "공석 발생", "테스트 메시지", NotificationChannel.FCM,
                LocalDateTime.of(2026, 9, 29, 10, 0));
        ScheduleSummary schedule = new ScheduleSummary(1L, "종강", null, null, null, null, "D-2");
        AnnouncementPreview announcement = new AnnouncementPreview(
                1L, "테스트 공지", "미리보기용 본문", true, LocalDateTime.now());

        when(userDashboardQuery.findCurrentUser()).thenReturn(Optional.of(user));
        when(notificationHistoryQuery.findLatestForUser(7L)).thenReturn(List.of(notification));
        when(primaryTimetableQuery.findPrimaryForCurrentUser()).thenReturn(Optional.of(
                new PrimaryTimetableSnapshot(10L, "대표 시간표", true, List.of(), List.of(), "0")));
        when(upcomingScheduleQuery.findUpcomingSchedules()).thenReturn(List.of(schedule));
        when(dashboardAnnouncementQuery.findDashboardAnnouncements(4)).thenReturn(List.of(announcement));

        DashboardSnapshotResponse result = dashboardService.getDashboardSnapshot();

        assertThat(result.getUser().name()).isEqualTo("홍길동");
        assertThat(result.getNotifications()).hasSize(1);
        assertThat(result.getNotifications().get(0).courseKey()).isEqualTo("TEST-101");
        assertThat(result.getPrimaryTimetable()).isNotNull();
        assertThat(result.getPrimaryTimetable().name()).isEqualTo("대표 시간표");
        assertThat(result.getUpcomingSchedules()).hasSize(1);
        assertThat(result.getAnnouncements()).hasSize(1);
        assertThat(result.getAnnouncements().get(0).title()).isEqualTo("테스트 공지");

        verify(userDashboardQuery).findCurrentUser();
        verify(notificationHistoryQuery).findLatestForUser(7L);
        verify(upcomingScheduleQuery).findUpcomingSchedules();
        verify(dashboardAnnouncementQuery).findDashboardAnnouncements(4);
    }

    @Test
    @DisplayName("비로그인 사용자는 공용 데이터만 조회한다")
    void getDashboardSnapshotForGuest() {
        ScheduleSummary schedule = new ScheduleSummary(1L, "종강", null, null, null, null, "D-2");
        AnnouncementPreview announcement = new AnnouncementPreview(
                1L, "테스트 공지", "미리보기용 본문", true, LocalDateTime.now());

        when(userDashboardQuery.findCurrentUser()).thenReturn(Optional.empty());
        when(upcomingScheduleQuery.findUpcomingSchedules()).thenReturn(List.of(schedule));
        when(dashboardAnnouncementQuery.findDashboardAnnouncements(4)).thenReturn(List.of(announcement));

        DashboardSnapshotResponse result = dashboardService.getDashboardSnapshot();

        assertThat(result.getUser()).isNull();
        assertThat(result.getNotifications()).isEmpty();
        assertThat(result.getPrimaryTimetable()).isNull();
        assertThat(result.getUpcomingSchedules()).hasSize(1);
        assertThat(result.getAnnouncements()).hasSize(1);

        verify(userDashboardQuery).findCurrentUser();
        verifyNoInteractions(notificationHistoryQuery, primaryTimetableQuery);
        verify(upcomingScheduleQuery).findUpcomingSchedules();
        verify(dashboardAnnouncementQuery).findDashboardAnnouncements(4);
    }
}
