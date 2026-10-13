package bhoon.sugang_helper.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.campus.announcement.dto.AnnouncementPreview;
import bhoon.sugang_helper.campus.schedule.dto.ScheduleSummary;
import bhoon.sugang_helper.dashboard.dto.DashboardNotificationResult;
import bhoon.sugang_helper.dashboard.dto.DashboardPrimaryTimetableResult;
import bhoon.sugang_helper.dashboard.dto.response.DashboardSnapshotResponse;
import bhoon.sugang_helper.planning.timetable.service.PrimaryTimetableQuery.CourseScheduleSnapshot;
import bhoon.sugang_helper.planning.timetable.service.PrimaryTimetableQuery.CourseSnapshot;
import bhoon.sugang_helper.planning.timetable.service.PrimaryTimetableQuery.CustomScheduleSnapshot;
import bhoon.sugang_helper.planning.timetable.service.PrimaryTimetableQuery.CustomScheduleTimeSnapshot;
import bhoon.sugang_helper.seatalert.enums.NotificationChannel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashboardSnapshotResponseContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void preservesDashboardJsonFieldsAcrossOwnerContracts() {
        DashboardSnapshotResponse response = DashboardSnapshotResponse.builder()
                .notifications(List.of(new DashboardNotificationResult(
                        1L, "CS101", "공석 발생", "알림", NotificationChannel.EMAIL,
                        LocalDateTime.of(2026, 9, 29, 10, 0))))
                .primaryTimetable(new DashboardPrimaryTimetableResult(
                        10L,
                        "대표 시간표",
                        true,
                        List.of(new CourseSnapshot(
                                "CS101", "자료구조", "홍길동", "월 1-A", "3", "전공", "공대",
                                List.of(new CourseScheduleSnapshot("MONDAY", "09:00", "10:00")))),
                        List.of(new CustomScheduleSnapshot(
                                20L, "동아리", "김교수",
                                List.of(new CustomScheduleTimeSnapshot(
                                        30L, "TUESDAY", LocalTime.of(18, 0), LocalTime.of(19, 0), "공대")))),
                        "3"))
                .upcomingSchedules(List.of(new ScheduleSummary(
                        40L, "수강 신청", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2),
                        LocalTime.of(9, 0), LocalTime.of(18, 0), "D-2")))
                .announcements(List.of(new AnnouncementPreview(
                        50L, "공지", "미리보기", true, LocalDateTime.of(2026, 9, 29, 9, 0))))
                .build();

        JsonNode json = objectMapper.valueToTree(response);

        assertThat(json.path("notifications").get(0).has("sentAt")).isTrue();
        assertThat(json.path("notifications").get(0).has("courseKey")).isTrue();
        assertThat(json.path("primaryTimetable").has("isPrimary")).isTrue();
        JsonNode course = json.path("primaryTimetable").path("courses").get(0);
        assertThat(course.has("courseKey")).isTrue();
        assertThat(course.path("schedules").get(0).path("dayOfWeek").asText()).isEqualTo("MONDAY");
        JsonNode customTime = json.path("primaryTimetable").path("customSchedules").get(0)
                .path("schedules").get(0);
        assertThat(customTime.has("startTime")).isTrue();
        assertThat(customTime.has("endTime")).isTrue();
        JsonNode schedule = json.path("upcomingSchedules").get(0);
        assertThat(schedule.path("startTime").asText()).isEqualTo("09:00");
        assertThat(schedule.path("dDay").asText()).isEqualTo("D-2");
        JsonNode announcement = json.path("announcements").get(0);
        assertThat(announcement.has("previewContent")).isTrue();
        assertThat(announcement.path("pinned").asBoolean()).isTrue();
    }
}
