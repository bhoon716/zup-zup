package bhoon.sugang_helper.domain.timetable.controller;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.domain.timetable.request.CustomScheduleRequest;
import bhoon.sugang_helper.domain.timetable.request.TimetableCourseRequest;
import bhoon.sugang_helper.domain.timetable.request.TimetableRequest;
import bhoon.sugang_helper.domain.timetable.response.TimetableDetailResponse;
import bhoon.sugang_helper.domain.timetable.response.TimetableResponse;
import bhoon.sugang_helper.domain.timetable.service.TimetableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/timetables")
@RequiredArgsConstructor
@Tag(name = "Timetable", description = "시간표 관련 API")
public class TimetableController {

    private final TimetableService timetableService;

    @Operation(summary = "시간표 생성", description = "새로운 시간표를 생성합니다.")
    @PostMapping
    public CommonResponse<TimetableResponse> createTimetable(@Valid @RequestBody TimetableRequest request) {
        return CommonResponse.success(timetableService.createTimetable(request), "시간표가 생성되었습니다.");
    }

    @Operation(summary = "내 시간표 목록 조회", description = "현재 사용자의 시간표 목록을 조회합니다.")
    @GetMapping
    public CommonResponse<List<TimetableResponse>> getMyTimetables() {
        return CommonResponse.success(timetableService.getMyTimetables(), "시간표 목록을 조회했습니다.");
    }

    @Operation(summary = "대표 시간표 조회", description = "현재 사용자의 대표 시간표를 조회합니다.")
    @GetMapping("/primary")
    public CommonResponse<TimetableDetailResponse> getPrimaryTimetable() {
        return CommonResponse.success(timetableService.getPrimaryTimetable(), "대표 시간표를 조회했습니다.");
    }

    @Operation(summary = "시간표 상세 조회", description = "시간표 ID로 상세 정보를 조회합니다.")
    @GetMapping("/{timetableId}")
    public CommonResponse<TimetableDetailResponse> getTimetableDetail(@PathVariable Long timetableId) {
        return CommonResponse.success(timetableService.getTimetableDetail(timetableId), "시간표 상세 정보를 조회했습니다.");
    }

    @Operation(summary = "시간표에 강의 추가", description = "지정한 시간표에 강의를 추가합니다.")
    @PostMapping("/{timetableId}/courses")
    public CommonResponse<Void> addCourse(
            @PathVariable Long timetableId,
            @Valid @RequestBody TimetableCourseRequest request) {
        timetableService.addCourse(timetableId, request.getCourseKey());
        return CommonResponse.success(null, "강좌가 시간표에 추가되었습니다.");
    }

    @Operation(summary = "시간표에서 강의 삭제", description = "지정한 시간표에서 강의를 삭제합니다.")
    @DeleteMapping("/{timetableId}/courses/{courseKey}")
    public CommonResponse<Void> deleteCourse(
            @PathVariable Long timetableId,
            @PathVariable String courseKey) {
        timetableService.deleteCourse(timetableId, courseKey);
        return CommonResponse.success(null, "강좌가 시간표에서 삭제되었습니다.");
    }

    @Operation(summary = "커스텀 일정 추가", description = "지정한 시간표에 커스텀 일정을 추가합니다.")
    @PostMapping("/{timetableId}/custom-schedules")
    public CommonResponse<Void> addCustomSchedule(
            @PathVariable Long timetableId,
            @Valid @RequestBody CustomScheduleRequest request) {
        timetableService.addCustomSchedule(timetableId, request);
        return CommonResponse.success(null, "일정이 추가되었습니다.");
    }

    @Operation(summary = "커스텀 일정 삭제", description = "지정한 시간표에서 커스텀 일정을 삭제합니다.")
    @DeleteMapping("/{timetableId}/custom-schedules/{scheduleId}")
    public CommonResponse<Void> deleteCustomSchedule(
            @PathVariable Long timetableId,
            @PathVariable Long scheduleId) {
        timetableService.deleteCustomSchedule(timetableId, scheduleId);
        return CommonResponse.success(null, "일정이 삭제되었습니다.");
    }

    @Operation(summary = "대표 시간표 변경", description = "지정한 시간표를 대표 시간표로 설정합니다.")
    @PatchMapping("/{timetableId}/primary")
    public CommonResponse<Void> setPrimary(@PathVariable Long timetableId) {
        timetableService.setPrimary(timetableId);
        return CommonResponse.success(null, "대표 시간표로 설정되었습니다.");
    }

    @Operation(summary = "시간표 삭제", description = "지정한 시간표를 삭제합니다.")
    @DeleteMapping("/{timetableId}")
    public CommonResponse<Void> deleteTimetable(@PathVariable Long timetableId) {
        timetableService.deleteTimetable(timetableId);
        return CommonResponse.success(null, "시간표가 삭제되었습니다.");
    }
}
