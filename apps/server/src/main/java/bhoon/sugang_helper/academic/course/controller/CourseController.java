package bhoon.sugang_helper.academic.course.controller;

import bhoon.sugang_helper.academic.course.dto.CourseSearchCondition;
import bhoon.sugang_helper.academic.course.dto.response.CourseDetailResponse;
import bhoon.sugang_helper.academic.course.dto.response.CourseResponse;
import bhoon.sugang_helper.academic.course.dto.response.CourseSeatHistoryResponse;
import bhoon.sugang_helper.academic.course.service.CourseService;
import bhoon.sugang_helper.common.response.CommonResponse;
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
    public ResponseEntity<CommonResponse<Slice<CourseResponse>>> searchCoursesByPost(
            @Valid @RequestBody CourseSearchCondition condition,
            @PageableDefault(size = 30) Pageable pageable) {
        Slice<CourseResponse> courses = courseService.searchCourses(condition, pageable);
        return CommonResponse.ok(courses, "과목 검색 결과입니다.");
    }

    @GetMapping("/{courseKey}/history")
    public ResponseEntity<CommonResponse<Slice<CourseSeatHistoryResponse>>> getCourseHistory(
            @PathVariable String courseKey,
            @PageableDefault(size = 30) Pageable pageable) {
        Slice<CourseSeatHistoryResponse> histories = courseService.getCourseHistory(courseKey, pageable);
        return CommonResponse.ok(histories, "해당 과목의 인원 변동 이력입니다.");
    }


    @GetMapping("/{courseKey}")
    public ResponseEntity<CommonResponse<CourseDetailResponse>> getCourse(
            @PathVariable String courseKey) {
        CourseDetailResponse course = courseService.getCourse(courseKey);
        return CommonResponse.ok(course, "과목 상세 정보입니다.");
    }

}
