package bhoon.sugang_helper.planning.timetable.controller;

import bhoon.sugang_helper.planning.timetable.dto.request.CustomScheduleRequest;
import bhoon.sugang_helper.planning.timetable.dto.request.TimetableCourseRequest;
import bhoon.sugang_helper.planning.timetable.dto.request.TimetableRequest;
import bhoon.sugang_helper.planning.timetable.dto.response.TimetableDetailResponse;
import bhoon.sugang_helper.planning.timetable.dto.response.TimetableResponse;
import bhoon.sugang_helper.planning.timetable.service.TimetableService;
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
public class TimetableController {

    private final TimetableService timetableService;

    @PostMapping
    public TimetableResponse createTimetable(@Valid @RequestBody TimetableRequest request) {
        return timetableService.createTimetable(request);
    }

    @GetMapping
    public List<TimetableResponse> getMyTimetables() {
        return timetableService.getMyTimetables();
    }

    @GetMapping("/primary")
    public TimetableDetailResponse getPrimaryTimetable() {
        return timetableService.getPrimaryTimetable();
    }

    @GetMapping("/{timetableId}")
    public TimetableDetailResponse getTimetableDetail(@PathVariable Long timetableId) {
        return timetableService.getTimetableDetail(timetableId);
    }

    @PostMapping("/{timetableId}/courses")
    public void addCourse(
            @PathVariable Long timetableId,
            @Valid @RequestBody TimetableCourseRequest request) {
        timetableService.addCourse(timetableId, request.getCourseKey());
    }

    @DeleteMapping("/{timetableId}/courses/{courseKey}")
    public void deleteCourse(
            @PathVariable Long timetableId,
            @PathVariable String courseKey) {
        timetableService.deleteCourse(timetableId, courseKey);
    }

    @PostMapping("/{timetableId}/custom-schedules")
    public void addCustomSchedule(
            @PathVariable Long timetableId,
            @Valid @RequestBody CustomScheduleRequest request) {
        timetableService.addCustomSchedule(timetableId, request);
    }

    @DeleteMapping("/{timetableId}/custom-schedules/{scheduleId}")
    public void deleteCustomSchedule(
            @PathVariable Long timetableId,
            @PathVariable Long scheduleId) {
        timetableService.deleteCustomSchedule(timetableId, scheduleId);
    }

    @PatchMapping("/{timetableId}/primary")
    public void setPrimary(@PathVariable Long timetableId) {
        timetableService.setPrimary(timetableId);
    }

    @DeleteMapping("/{timetableId}")
    public void deleteTimetable(@PathVariable Long timetableId) {
        timetableService.deleteTimetable(timetableId);
    }
}
