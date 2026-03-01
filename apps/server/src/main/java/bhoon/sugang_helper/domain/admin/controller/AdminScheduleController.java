package bhoon.sugang_helper.domain.admin.controller;

import bhoon.sugang_helper.domain.schedule.request.ScheduleRequest;
import bhoon.sugang_helper.domain.schedule.response.ScheduleResponse;
import bhoon.sugang_helper.domain.schedule.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@SecurityRequirement(name = "Cookie")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Schedules", description = "어드민 일정 관리 API")
public class AdminScheduleController {

    private final ScheduleService scheduleService;

    /**
     * 전체 일정의 목록(기간이 지난 일정 포함)을 최신순으로 반환합니다.
     */
    @Operation(summary = "모든 일정 목록 조회", description = "만료된 일정을 포함해 전체 일정을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<ScheduleResponse>> getAllSchedules() {
        return ResponseEntity.ok(scheduleService.getAllSchedules());
    }

    /**
     * 주어진 데이터(ScheduleRequest)를 바탕으로 데이터베이스에 새로운 일정을 추가합니다.
     */
    @Operation(summary = "신규 일정 생성")
    @PostMapping
    public ResponseEntity<ScheduleResponse> createSchedule(@RequestBody @Valid ScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.createSchedule(request));
    }

    @Operation(summary = "기존 일정 수정")
    @PutMapping("/{id}")
    public ResponseEntity<ScheduleResponse> updateSchedule(@PathVariable Long id,
            @RequestBody @Valid ScheduleRequest request) {
        return ResponseEntity.ok(scheduleService.updateSchedule(id, request));
    }

    /**
     * 일정 ID로 일정을 찾고 이를 데이터베이스에서 영구히 삭제합니다.
     */
    @Operation(summary = "일정 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }
}
