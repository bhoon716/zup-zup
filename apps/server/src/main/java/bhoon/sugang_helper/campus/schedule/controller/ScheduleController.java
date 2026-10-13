package bhoon.sugang_helper.campus.schedule.controller;

import bhoon.sugang_helper.campus.schedule.dto.response.ScheduleResponse;
import bhoon.sugang_helper.campus.schedule.service.ScheduleService;
import bhoon.sugang_helper.common.response.CommonResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    /**
     * 사용자의 대시보드에서 보일 최신 예정 일정들을 불러옵니다.
     */
    @GetMapping
    public ResponseEntity<CommonResponse<List<ScheduleResponse>>> getUpcomingSchedules() {
        return CommonResponse.ok(scheduleService.getUpcomingSchedules(), "예정된 주요 일정 목록입니다.");
    }
}
