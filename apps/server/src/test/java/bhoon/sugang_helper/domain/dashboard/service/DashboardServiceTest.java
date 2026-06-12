package bhoon.sugang_helper.domain.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.domain.announcement.entity.Announcement;
import bhoon.sugang_helper.domain.announcement.repository.AnnouncementRepository;
import bhoon.sugang_helper.domain.dashboard.response.DashboardSnapshotResponse;
import bhoon.sugang_helper.domain.notification.entity.NotificationHistory;
import bhoon.sugang_helper.domain.notification.repository.NotificationHistoryRepository;
import bhoon.sugang_helper.domain.notification.sender.NotificationChannel;
import bhoon.sugang_helper.domain.schedule.entity.Schedule;
import bhoon.sugang_helper.domain.schedule.response.ScheduleResponse;
import bhoon.sugang_helper.domain.schedule.service.ScheduleService;
import bhoon.sugang_helper.domain.timetable.response.TimetableDetailResponse;
import bhoon.sugang_helper.domain.timetable.service.TimetableService;
import bhoon.sugang_helper.domain.user.entity.Role;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.service.UserService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private NotificationHistoryRepository notificationHistoryRepository;

    @Mock
    private TimetableService timetableService;

    @Mock
    private ScheduleService scheduleService;

    @Mock
    private AnnouncementRepository announcementRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("대시보드 스냅샷을 한 번에 조회한다")
    void getDashboardSnapshot() {
        // given
        User user = User.builder()
                .id(7L)
                .name("홍길동")
                .email("user@example.com")
                .role(Role.USER)
                .emailEnabled(true)
                .webPushEnabled(true)
                .fcmEnabled(true)
                .discordEnabled(false)
                .onboardingCompleted(true)
                .build();

        NotificationHistory notification = NotificationHistory.builder()
                .userId(7L)
                .courseKey("TEST-101")
                .title("공석 발생")
                .message("테스트 메시지")
                .channel(NotificationChannel.FCM)
                .build();

        TimetableDetailResponse timetable = TimetableDetailResponse.builder()
                .id(10L)
                .name("대표 시간표")
                .isPrimary(true)
                .courses(List.of())
                .customSchedules(List.of())
                .totalCredits("0")
                .build();

        Schedule schedule = Schedule.builder()
                .scheduleType("종강")
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(18, 0))
                .build();

        Announcement announcement = Announcement.builder()
                .title("테스트 공지")
                .content("미리보기용 본문")
                .pinned(true)
                .published(true)
                .build();

        when(userService.getCurrentUserOrNull()).thenReturn(Optional.of(user));
        when(notificationHistoryRepository.findTop3ByUserIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(notification));
        when(timetableService.getPrimaryTimetable()).thenReturn(timetable);
        when(scheduleService.getUpcomingSchedules()).thenReturn(List.of(ScheduleResponse.from(schedule)));
        when(announcementRepository.findByPublishedTrueOrderByPinnedDescCreatedAtDesc(PageRequest.of(0, 4)))
                .thenReturn(List.of(announcement));

        // when
        DashboardSnapshotResponse result = dashboardService.getDashboardSnapshot();

        // then
        assertThat(result.getUser().getName()).isEqualTo("홍길동");
        assertThat(result.getNotifications()).hasSize(1);
        assertThat(result.getNotifications().get(0).getCourseKey()).isEqualTo("TEST-101");
        assertThat(result.getPrimaryTimetable()).isNotNull();
        assertThat(result.getPrimaryTimetable().getName()).isEqualTo("대표 시간표");
        assertThat(result.getUpcomingSchedules()).hasSize(1);
        assertThat(result.getAnnouncements()).hasSize(1);
        assertThat(result.getAnnouncements().get(0).getTitle()).isEqualTo("테스트 공지");

        verify(userService).getCurrentUserOrNull();
        verify(notificationHistoryRepository).findTop3ByUserIdOrderByCreatedAtDesc(7L);
        verify(announcementRepository).findByPublishedTrueOrderByPinnedDescCreatedAtDesc(eq(PageRequest.of(0, 4)));
    }

    @Test
    @DisplayName("비로그인 사용자는 공용 데이터만 조회한다")
    void getDashboardSnapshotForGuest() {
        // given
        Schedule schedule = Schedule.builder()
                .scheduleType("종강")
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(18, 0))
                .build();

        Announcement announcement = Announcement.builder()
                .title("테스트 공지")
                .content("미리보기용 본문")
                .pinned(true)
                .published(true)
                .build();

        when(userService.getCurrentUserOrNull()).thenReturn(Optional.empty());
        when(scheduleService.getUpcomingSchedules()).thenReturn(List.of(ScheduleResponse.from(schedule)));
        when(announcementRepository.findByPublishedTrueOrderByPinnedDescCreatedAtDesc(PageRequest.of(0, 4)))
                .thenReturn(List.of(announcement));

        // when
        DashboardSnapshotResponse result = dashboardService.getDashboardSnapshot();

        // then
        assertThat(result.getUser()).isNull();
        assertThat(result.getNotifications()).isEmpty();
        assertThat(result.getPrimaryTimetable()).isNull();
        assertThat(result.getUpcomingSchedules()).hasSize(1);
        assertThat(result.getAnnouncements()).hasSize(1);

        verify(userService).getCurrentUserOrNull();
        verifyNoInteractions(notificationHistoryRepository, timetableService);
        verify(announcementRepository).findByPublishedTrueOrderByPinnedDescCreatedAtDesc(eq(PageRequest.of(0, 4)));
    }
}
