package bhoon.sugang_helper.campus.schedule.controller;

import bhoon.sugang_helper.campus.schedule.dto.request.ScheduleRequest;
import bhoon.sugang_helper.campus.schedule.dto.response.ScheduleResponse;
import bhoon.sugang_helper.campus.schedule.service.ScheduleService;
import bhoon.sugang_helper.common.response.CommonResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/schedules")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminScheduleController {

    private final ScheduleService scheduleService;

    /**
     * 전체 일정의 목록(기간이 지난 일정 포함)을 최신순으로 반환합니다.
     */
    @GetMapping
    public ResponseEntity<CommonResponse<List<ScheduleResponse>>> getAllSchedules() {
        return CommonResponse.ok(scheduleService.getAllSchedules(), "전체 일정 목록입니다.");
    }

    /**
     * 주어진 데이터(ScheduleRequest)를 바탕으로 데이터베이스에 새로운 일정을 추가합니다.
     */
    @PostMapping
    public ResponseEntity<CommonResponse<ScheduleResponse>> createSchedule(
            @RequestBody @Valid ScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.success(scheduleService.createSchedule(request), "일정을 생성했습니다."));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<ScheduleResponse>> updateSchedule(@PathVariable Long id,
                                                                           @RequestBody @Valid ScheduleRequest request) {
        return CommonResponse.ok(scheduleService.updateSchedule(id, request), "일정을 수정했습니다.");
    }

    /**
     * 일정 ID로 일정을 찾고 이를 데이터베이스에서 영구히 삭제합니다.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }
}
