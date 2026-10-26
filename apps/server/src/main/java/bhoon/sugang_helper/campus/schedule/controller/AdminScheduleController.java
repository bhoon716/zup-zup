package bhoon.sugang_helper.campus.schedule.controller;

import bhoon.sugang_helper.campus.schedule.dto.request.ScheduleRequest;
import bhoon.sugang_helper.campus.schedule.dto.response.ScheduleResponse;
import bhoon.sugang_helper.campus.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
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

    @GetMapping
    public ResponseEntity<List<ScheduleResponse>> getAllSchedules() {
        return ResponseEntity.ok(scheduleService.getAllSchedules());
    }

    @PostMapping
    public ResponseEntity<ScheduleResponse> createSchedule(
            @RequestBody @Valid ScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scheduleService.createSchedule(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScheduleResponse> updateSchedule(@PathVariable Long id,
                                                            @RequestBody @Valid ScheduleRequest request) {
        return ResponseEntity.ok(scheduleService.updateSchedule(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/restore")
    public ResponseEntity<ScheduleResponse> restoreSchedule(@PathVariable Long id) {
        return ResponseEntity.ok(scheduleService.restoreSchedule(id));
    }
}
