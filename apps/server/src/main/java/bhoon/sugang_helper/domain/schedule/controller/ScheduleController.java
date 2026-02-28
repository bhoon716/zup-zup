package bhoon.sugang_helper.domain.schedule.controller;

import bhoon.sugang_helper.domain.schedule.response.ScheduleResponse;
import bhoon.sugang_helper.domain.schedule.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
@Tag(name = "Schedules", description = "일정 조회 API (유저용)")
public class ScheduleController {

    private final ScheduleService scheduleService;

    /**
     * 사용자의 대시보드에서 보일 최신 예정 일정들을 불러옵니다.
     */
    @Operation(summary = "예정된 주요 일정 목록 조회", description = "메인 대시보드 표시용. 날짜가 지난 일정은 제외됩니다.")
    @GetMapping
    public ResponseEntity<List<ScheduleResponse>> getUpcomingSchedules() {
        return ResponseEntity.ok(scheduleService.getUpcomingSchedules());
    }
}
