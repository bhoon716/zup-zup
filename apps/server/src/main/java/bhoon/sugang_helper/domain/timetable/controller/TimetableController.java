package bhoon.sugang_helper.domain.timetable.controller;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.domain.timetable.request.CustomScheduleRequest;
import bhoon.sugang_helper.domain.timetable.request.TimetableCourseRequest;
import bhoon.sugang_helper.domain.timetable.request.TimetableRequest;
import bhoon.sugang_helper.domain.timetable.response.TimetableDetailResponse;
import bhoon.sugang_helper.domain.timetable.response.TimetableResponse;
import bhoon.sugang_helper.domain.timetable.service.TimetableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "시간표 생성 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
                    {
                      "code": "SUCCESS",
                      "message": "시간표가 생성되었습니다.",
                      "data": {
                        "id": 1,
                        "name": "2026학년도 1학기",
                        "isPrimary": true,
                        "createdAt": "2026-02-21T07:20:00"
                      }
                    }
                    """)))
    })
    @PostMapping
    public CommonResponse<TimetableResponse> createTimetable(@Valid @RequestBody TimetableRequest request) {
        return CommonResponse.success(timetableService.createTimetable(request), "시간표가 생성되었습니다.");
    }

    @Operation(summary = "내 시간표 목록 조회", description = "현재 사용자의 시간표 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "목록 조회 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
                    {
                      "code": "SUCCESS",
                      "message": "시간표 목록을 조회했습니다.",
                      "data": [
                        { "id": 1, "name": "월요일 개강", "isPrimary": true },
                        { "id": 2, "name": "꿀강 시간표", "isPrimary": false }
                      ]
                    }
                    """)))
    })
    @GetMapping
    public CommonResponse<List<TimetableResponse>> getMyTimetables() {
        return CommonResponse.success(timetableService.getMyTimetables(), "시간표 목록을 조회했습니다.");
    }

    @Operation(summary = "대표 시간표 조회", description = "현재 사용자의 대표 시간표를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
                    {
                      "code": "SUCCESS",
                      "message": "대표 시간표를 조회했습니다.",
                      "data": {
                        "id": 1,
                        "name": "메인 시간표",
                        "courses": [
                          { "courseKey": "2026:10:CLTR01:01", "name": "기초프로그래밍", "classTime": "월1,2" }
                        ],
                        "customSchedules": []
                      }
                    }
                    """))),
            @ApiResponse(responseCode = "404", description = "생성된 시간표 없음", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
                    {
                      "code": "TIMETABLE_NOT_FOUND",
                      "message": "등록된 시간표가 없습니다.",
                      "data": null
                    }
                    """)))
    })
    @GetMapping("/primary")
    public CommonResponse<TimetableDetailResponse> getPrimaryTimetable() {
        return CommonResponse.success(timetableService.getPrimaryTimetable(), "대표 시간표를 조회했습니다.");
    }

    @Operation(summary = "시간표 상세 조회", description = "시간표 ID로 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상세 조회 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
                    {
                      "code": "SUCCESS",
                      "message": "시간표 상세 정보를 조회했습니다.",
                      "data": {
                        "id": 1,
                        "name": "2026-1",
                        "courses": [
                          { "courseKey": "2026:10:CLTR01:01", "name": "기초프로그래밍", "classTime": "월1,2" }
                        ],
                        "customSchedules": [
                          { "id": 10, "title": "동아리", "startTime": "18:00:00", "endTime": "19:00:00" }
                        ]
                      }
                    }
                    """)))
    })
    @GetMapping("/{timetableId}")
    public CommonResponse<TimetableDetailResponse> getTimetableDetail(@PathVariable Long timetableId) {
        return CommonResponse.success(timetableService.getTimetableDetail(timetableId), "시간표 상세 정보를 조회했습니다.");
    }

    @Operation(summary = "시간표에 강의 추가", description = "지정한 시간표에 강의를 추가합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "강의 추가 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
                    {
                      "code": "SUCCESS",
                      "message": "강좌가 시간표에 추가되었습니다.",
                      "data": null
                    }
                    """))),
            @ApiResponse(responseCode = "400", description = "강의 시간 중복", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
                    {
                      "code": "COURSE_TIME_CONFLICT",
                      "message": "이미 등록된 강의와 시간이 겹칩니다.",
                      "data": null
                    }
                    """)))
    })
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "추가 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
                    {
                      "code": "SUCCESS",
                      "message": "일정이 추가되었습니다.",
                      "data": null
                    }
                    """))),
            @ApiResponse(responseCode = "400", description = "시간대 중복 오류", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
                    {
                      "code": "SCHEDULE_CONFLICT",
                      "message": "기존 일정과 시간이 겹칩니다.",
                      "data": null
                    }
                    """)))
    })
    @PostMapping("/{timetableId}/custom-schedules")
    public CommonResponse<Void> addCustomSchedule(
            @PathVariable Long timetableId,
            @Valid @RequestBody CustomScheduleRequest request) {
        timetableService.addCustomSchedule(timetableId, request);
        return CommonResponse.success(null, "일정이 추가되었습니다.");
    }

    @Operation(summary = "커스텀 일정 삭제", description = "지정한 시간표에서 커스텀 일정을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "삭제 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
                    {
                      "code": "SUCCESS",
                      "message": "일정이 삭제되었습니다.",
                      "data": null
                    }
                    """)))
    })
    @DeleteMapping("/{timetableId}/custom-schedules/{scheduleId}")
    public CommonResponse<Void> deleteCustomSchedule(
            @PathVariable Long timetableId,
            @PathVariable Long scheduleId) {
        timetableService.deleteCustomSchedule(timetableId, scheduleId);
        return CommonResponse.success(null, "일정이 삭제되었습니다.");
    }

    @Operation(summary = "대표 시간표 변경", description = "지정한 시간표를 대표 시간표로 설정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "설정 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
                    {
                      "code": "SUCCESS",
                      "message": "대표 시간표로 설정되었습니다.",
                      "data": null
                    }
                    """)))
    })
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
