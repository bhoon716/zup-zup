package bhoon.sugang_helper.dashboard.application;

import bhoon.sugang_helper.announcement.application.AnnouncementListResponse;
import bhoon.sugang_helper.dashboard.application.result.DashboardPrimaryTimetableResult;
import bhoon.sugang_helper.dashboard.application.result.DashboardUserResult;
import bhoon.sugang_helper.notification.application.NotificationHistoryResponse;
import bhoon.sugang_helper.schedule.application.ScheduleResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "사용자 대시보드 스냅샷 응답 DTO")
public class DashboardSnapshotResponse {

    @Schema(description = "사용자 프로필")
    private DashboardUserResult user;

    @Schema(description = "최근 알림 목록")
    private List<NotificationHistoryResponse> notifications;

    @Schema(description = "대표 시간표")
    private DashboardPrimaryTimetableResult primaryTimetable;

    @Schema(description = "다가오는 주요 일정")
    private List<ScheduleResponse> upcomingSchedules;

    @Schema(description = "최근 공지사항")
    private List<AnnouncementListResponse> announcements;
}
