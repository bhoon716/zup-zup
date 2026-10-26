package bhoon.sugang_helper.academic.course.controller;

import bhoon.sugang_helper.academic.course.dto.request.CourseSearchCondition;
import bhoon.sugang_helper.academic.course.dto.response.CourseDetailResponse;
import bhoon.sugang_helper.academic.course.dto.response.CourseResponse;
import bhoon.sugang_helper.academic.course.dto.response.CourseSeatHistoryResponse;
import bhoon.sugang_helper.academic.course.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping("/search")
    public ResponseEntity<Slice<CourseResponse>> searchCoursesByPost(
            @Valid @RequestBody CourseSearchCondition condition,
            @PageableDefault(size = 30) Pageable pageable) {
        Slice<CourseResponse> courses = courseService.searchCourses(condition, pageable);
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/{courseKey}/history")
    public ResponseEntity<Slice<CourseSeatHistoryResponse>> getCourseHistory(
            @PathVariable String courseKey,
            @PageableDefault(size = 30) Pageable pageable) {
        Slice<CourseSeatHistoryResponse> histories = courseService.getCourseHistory(courseKey, pageable);
        return ResponseEntity.ok(histories);
    }


    @GetMapping("/{courseKey}")
    public ResponseEntity<CourseDetailResponse> getCourse(
            @PathVariable String courseKey) {
        CourseDetailResponse course = courseService.getCourse(courseKey);
        return ResponseEntity.ok(course);
    }

}
