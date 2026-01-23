package bhoon.sugang_helper.domain.course.controller;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.domain.course.request.CourseSearchCondition;
import bhoon.sugang_helper.domain.course.response.CourseResponse;
import bhoon.sugang_helper.domain.course.response.CourseSeatHistoryResponse;
import bhoon.sugang_helper.domain.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public ResponseEntity<CommonResponse<Page<CourseResponse>>> searchCourses(
            CourseSearchCondition condition, Pageable pageable) {
        Page<CourseResponse> courses = courseService.searchCourses(condition, pageable);
        return CommonResponse.ok(courses, "과목 검색 결과입니다.");
    }

    @GetMapping("/{courseKey}/history")
    public ResponseEntity<CommonResponse<List<CourseSeatHistoryResponse>>> getCourseHistory(
            @PathVariable String courseKey) {
        List<CourseSeatHistoryResponse> histories = courseService.getCourseHistory(courseKey);
        return CommonResponse.ok(histories, "해당 과목의 인원 변동 이력입니다.");
    }
}
